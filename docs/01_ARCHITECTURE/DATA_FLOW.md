# Data Flow

> humanOS Native Android -- Data Flow Patterns
> Last updated: 2026-06-06

## Repository Pattern

Every data entity in humanOS Android flows through a Repository. The Repository is the single API surface that ViewModels (or use cases in Phase 2+) call. It hides whether data comes from Room, Retrofit, DataStore, or a combination.

```
Compose UI
    |  collectAsState()
    v
ViewModel (StateFlow<UiState>)
    |  suspend fun / Flow
    v
UseCase (Phase 2+, optional orchestration)
    |  suspend fun / Flow
    v
Repository (interface in core-model or domain, impl in data-*)
   / \
  /   \
Room   Retrofit (via integration-* Gateway)
(local)  (remote)
```

### Contract

```kotlin
interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTask(id: String): Flow<Task?>
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(id: String): Result<Unit>
    suspend fun syncTasks(): Result<SyncResult>
}
```

- **Read operations** return `Flow` so the UI reacts to database changes automatically.
- **Write operations** return `Result<T>` (kotlin.Result or a sealed wrapper) so errors propagate cleanly.
- **Sync operations** are explicit and triggered by WorkManager, pull-to-refresh, or app foreground.

## NetworkBoundResource Pattern

The core synchronization pattern for any entity that exists both locally and remotely.

```
        [START]
           |
    +------v-------+
    | Emit cached   |  Room DAO.observe() → Flow<T?>
    | from Room     |
    +------+-------+
           |
    +------v-------+
    | Should fetch? |  Based on staleness, connectivity, user action
    +------+-------+
       yes |     | no
           v     +------→ [DONE - cached data is fresh enough]
    +------v-------+
    | Fetch from    |  Retrofit call via Gateway interface
    | network       |
    +------+-------+
     success|    | failure
           v     +------→ Emit cached + error state (stale indicator)
    +------v-------+
    | Map DTOs to   |  integration DTO → core-model entity
    | entities      |
    +------+-------+
           |
    +------v-------+
    | Save to Room  |  DAO.upsert() inside @Transaction
    +------+-------+
           |
    +------v-------+
    | Room Flow     |  Automatic re-emission to collectors
    | re-emits      |
    +------+-------+
           |
        [DONE]
```

### Implementation Sketch

```kotlin
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    crossinline onFetchFailed: (Throwable) -> Unit = { }
): Flow<Resource<ResultType>> = flow {
    val data = query().first()
    emit(Resource.Loading(data))

    if (shouldFetch(data)) {
        try {
            val fetchedData = fetch()
            saveFetchResult(fetchedData)
            emitAll(query().map { Resource.Success(it) })
        } catch (t: Throwable) {
            onFetchFailed(t)
            emitAll(query().map { Resource.Error(t, it) })
        }
    } else {
        emitAll(query().map { Resource.Success(it) })
    }
}
```

### Resource Wrapper

```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val error: Throwable, val cachedData: T? = null) : Resource<T>()
    data class Loading<T>(val cachedData: T? = null) : Resource<T>()
}
```

## SSE Streaming (QueBot Chat)

QueBot chat uses Server-Sent Events for real-time token streaming. This does NOT follow the NetworkBoundResource pattern -- it is a live stream, not a cache-sync cycle.

```
Compose ChatScreen
    |  collectAsState()
    v
ChatViewModel
    |  chatRepository.sendMessage(caseId, prompt)
    v
ChatRepository
    |  quebotGateway.streamChat(caseId, prompt)
    v
QuebotGateway (OkHttp EventSource)
    |
    v  Flow<SseEvent>
    |
    |  SseEvent.Delta(token) → append to message buffer
    |  SseEvent.SearchExecuted(query, results) → show search card
    |  SseEvent.Done(fullResponse) → finalize, save to Room
    |  SseEvent.Error(code, message) → show error, offer retry
```

### SSE Event Types

```kotlin
sealed class SseEvent {
    data class Status(val status: String) : SseEvent()
    data class Delta(val token: String) : SseEvent()
    data class SearchExecuted(
        val query: String,
        val resultCount: Int
    ) : SseEvent()
    data class Done(val fullResponse: String) : SseEvent()
    data class Error(val code: Int, val message: String) : SseEvent()
}
```

### Flow Wrapper for OkHttp EventSource

```kotlin
fun streamChat(caseId: String, prompt: String): Flow<SseEvent> = callbackFlow {
    val request = Request.Builder()
        .url("$baseUrl/api/v1/cases/$caseId/chat")
        .post(/* prompt body */)
        .addHeader("Authorization", "Bearer $firebaseToken")
        .build()

    val source = EventSources.createFactory(okHttpClient)
        .newEventSource(request, object : EventSourceListener() {
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                trySend(parseSseEvent(type, data))
            }
            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("SSE connection failed"))
            }
            override fun onClosed(es: EventSource) {
                close()
            }
        })

    awaitClose { source.cancel() }
}
```

## DataStore Flows

User preferences and session state use Jetpack DataStore, which also exposes `Flow`.

```kotlin
// Reading
val themeFlow: Flow<ThemePreference> = dataStore.data
    .map { prefs -> prefs[THEME_KEY]?.let { ThemePreference.valueOf(it) } ?: ThemePreference.SYSTEM }

// Writing
suspend fun setTheme(theme: ThemePreference) {
    dataStore.edit { prefs -> prefs[THEME_KEY] = theme.name }
}
```

DataStore is used for:
- User session tokens (encrypted via core-security before storage)
- Theme preference (light / dark / system)
- Onboarding completed flag
- Last sync timestamps per entity type
- Feature flags (fetched from remote config, cached locally)

## Data Flow Summary by Screen (Phase 1)

| Screen | Data Source | Flow Type | Sync |
|---|---|---|---|
| Dashboard | Room (tasks, captures) + DataStore (user name) | `StateFlow<DashboardUiState>` | None (Phase 1) |
| Capture | Local only (Room insert) | One-shot `suspend` | None (Phase 1) |
| Settings | DataStore (prefs) + data-auth (session) | `StateFlow<SettingsUiState>` | None (Phase 1) |

## Conflict Resolution Strategy (Phase 2+)

When sync detects conflicts (local edit + remote edit on same entity):

1. **Last-write-wins** with server timestamp as tiebreaker.
2. **Soft-delete**: Deleted items get `deletedAt` timestamp, not hard-deleted.
3. **Merge fields**: For multi-field entities (e.g., Task), field-level merge when possible.
4. **Conflict log**: Unresolvable conflicts saved to `SyncConflict` table for manual review.
