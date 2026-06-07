# Adapter Strategy

> humanOS Native Android -- Gateway Pattern and Integration Isolation
> Last updated: 2026-06-06

## Principle

No feature module, data module, or domain module ever talks directly to a real backend. Every external system is accessed through a Gateway interface. The real implementation uses Retrofit/OkHttp. The fake implementation returns canned data. Hilt binds the correct one depending on build variant.

This guarantees:
1. **Testability** -- Unit tests never need a network. Fakes are deterministic.
2. **Offline development** -- Phase 1 works entirely with fakes. Phase 2 wires real implementations incrementally.
3. **Backend independence** -- If the HumanOS API changes a response format, only the DTO mapping in the integration module changes. Feature modules are unaffected.
4. **Swappability** -- A new backend (e.g., replacing QueBot with a different AI provider) requires only a new integration module implementing the same Gateway interface.

## Module Structure

Each integration module contains exactly four layers:

```
:integration-humanos/
    src/main/kotlin/com/humanos/integration/humanos/
        api/                        # Layer 1: Retrofit service
            HumanosApiService.kt
        dto/                        # Layer 2: DTOs
            TaskDto.kt
            AppointmentDto.kt
            ContextSnapshotDto.kt
            MobileAuthDtos.kt
            DailyReviewDto.kt
            Mappers.kt              # DTO ↔ domain model extensions
        gateway/                    # Layer 3: Gateway interface + real impl
            HumanosGateway.kt       # Interface
            HumanosGatewayImpl.kt   # Real implementation
        di/                         # Hilt module
            HumanosModule.kt
    src/test/kotlin/
        fake/                       # Layer 4: Fake implementation
            FakeHumanosGateway.kt
        gateway/
            HumanosGatewayImplTest.kt
```

## Layer 1: Retrofit Service Interface

The Retrofit interface declares HTTP methods with kotlinx.serialization.

```kotlin
interface HumanosApiService {

    @POST("auth/mobile/exchange")
    suspend fun exchangeToken(
        @Body request: MobileAuthExchangeRequest,
    ): MobileAuthExchangeResponse

    @GET("context/snapshot")
    suspend fun getContextSnapshot(
        @Query("since") since: String? = null,
        @Query("limit") limit: Int = 500,
        @Query("cursor") cursor: String? = null,
    ): ContextSnapshotResponse

    @POST("context/batch")
    suspend fun pushContextBatch(
        @Body request: ContextBatchRequest,
    ): ContextBatchResponse

    @GET("tasks")
    suspend fun getTasks(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: String? = null,
        @Query("sortBy") sortBy: String? = null,
    ): PaginatedResponse<TaskDto>

    @POST("tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
    ): TaskDto

    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body request: UpdateTaskRequest,
    ): TaskDto

    @DELETE("tasks/{id}")
    suspend fun deleteTask(
        @Path("id") id: String,
    )

    @GET("appointments")
    suspend fun getAppointments(
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<AppointmentDto>

    @GET("daily-review/{date}")
    suspend fun getDailyReview(
        @Path("date") date: String,
    ): DailyReviewDto

    @POST("daily-review/generate")
    suspend fun generateDailyReview(
        @Body request: GenerateDailyReviewRequest,
    ): DailyReviewDto
}
```

The Retrofit service interface is **never exposed** outside the integration module. Only the Gateway interface is visible.

## Layer 2: DTO Classes

DTOs are plain `@Serializable` data classes that mirror the JSON structure exactly. They live in the integration module and are NOT shared with feature modules.

```kotlin
@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val dueDate: String? = null,
    val assigneeId: String? = null,
    val projectId: String? = null,
    val contextNodeIds: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null,
)
```

### Mappers

Extension functions convert between DTOs and domain models:

```kotlin
// dto/Mappers.kt

fun TaskDto.toTask(): Task = Task(
    id = id,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    dueDate = dueDate?.let { LocalDate.parse(it) },
    assigneeId = assigneeId,
    projectId = projectId,
    contextNodeIds = contextNodeIds,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    completedAt = completedAt?.let { Instant.parse(it) },
    syncState = SyncState.SYNCED,
)

fun Task.toCreateRequest(): CreateTaskRequest = CreateTaskRequest(
    title = title,
    description = description,
    priority = priority.name,
    dueDate = dueDate?.toString(),
    assigneeId = assigneeId,
    projectId = projectId,
    contextNodeIds = contextNodeIds,
)
```

Mappers handle:
- String → enum conversion (with fallback to UNKNOWN for forward compatibility)
- String → temporal type parsing (ISO 8601)
- Null safety mismatches between server and client models
- Default values for fields the server may omit

## Layer 3: Gateway Interface

The Gateway interface is the public API of the integration module. It uses domain model types, not DTOs.

```kotlin
interface HumanosGateway {
    suspend fun exchangeToken(firebaseToken: String, deviceId: String): Result<AuthSession>
    suspend fun getContextSnapshot(since: Instant?): Result<ContextSnapshot>
    suspend fun pushContextBatch(nodes: List<ContextNode>, edges: List<ContextEdge>): Result<BatchResult>
    suspend fun getTasks(page: Int, pageSize: Int, statusFilter: Set<TaskStatus>?): Result<PaginatedResult<Task>>
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(id: String): Result<Unit>
    suspend fun getAppointments(from: Instant, to: Instant): Result<List<Appointment>>
    suspend fun getDailyReview(date: LocalDate): Result<DailyReview>
    suspend fun generateDailyReview(date: LocalDate): Result<DailyReview>
}
```

### Real Implementation

```kotlin
@Singleton
class HumanosGatewayImpl @Inject constructor(
    private val api: HumanosApiService,
) : HumanosGateway {

    override suspend fun getTasks(
        page: Int,
        pageSize: Int,
        statusFilter: Set<TaskStatus>?,
    ): Result<PaginatedResult<Task>> = runCatching {
        val statusParam = statusFilter?.joinToString(",") { it.name }
        val response = api.getTasks(page, pageSize, statusParam)
        PaginatedResult(
            data = response.data.map { it.toTask() },
            total = response.total,
            page = response.page,
            pageSize = response.pageSize,
        )
    }

    // ... other methods follow the same pattern
}
```

The real implementation:
- Delegates to the Retrofit service
- Maps DTOs to domain models via mapper extensions
- Wraps everything in `Result` (catches network exceptions)
- Never contains business logic

## Layer 4: Fake Implementation

The fake returns deterministic data without any network call.

```kotlin
@Singleton
class FakeHumanosGateway @Inject constructor() : HumanosGateway {

    private val fakeTasks = mutableListOf(
        Task(
            id = "task-001",
            title = "Review project proposal",
            description = "Read and provide feedback on the Q3 project proposal",
            status = TaskStatus.TODO,
            priority = TaskPriority.HIGH,
            dueDate = LocalDate.now().plusDays(2),
            createdAt = Instant.now().minus(Duration.ofDays(1)),
            updatedAt = Instant.now().minus(Duration.ofDays(1)),
            syncState = SyncState.SYNCED,
        ),
        Task(
            id = "task-002",
            title = "Schedule team meeting",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.MEDIUM,
            createdAt = Instant.now().minus(Duration.ofDays(3)),
            updatedAt = Instant.now(),
            syncState = SyncState.SYNCED,
        ),
    )

    override suspend fun getTasks(
        page: Int,
        pageSize: Int,
        statusFilter: Set<TaskStatus>?,
    ): Result<PaginatedResult<Task>> = Result.success(
        PaginatedResult(
            data = fakeTasks
                .filter { statusFilter == null || it.status in statusFilter }
                .drop((page - 1) * pageSize)
                .take(pageSize),
            total = fakeTasks.size,
            page = page,
            pageSize = pageSize,
        )
    )

    override suspend fun createTask(task: Task): Result<Task> {
        val created = task.copy(
            id = "task-${fakeTasks.size + 1}",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            syncState = SyncState.SYNCED,
        )
        fakeTasks.add(created)
        return Result.success(created)
    }

    // ... other methods with similar canned behavior
}
```

### Fake Behavior Rules

1. Fakes are **stateful** within a session (creates/updates persist in memory).
2. Fakes simulate realistic latency (`delay(200)`) to test loading states.
3. Fakes can be configured to return errors for specific calls (testing error handling).
4. Fakes use the same data as `TestFixtures` in `testing-common` for consistency.

## QueBot Gateway

The QueBot integration module follows the same four-layer structure, with one addition: SSE streaming.

```kotlin
interface QuebotGateway {
    suspend fun getCases(): Result<List<QuebotCase>>
    suspend fun createCase(title: String): Result<QuebotCase>
    suspend fun getCaseHistory(caseId: String): Result<List<QuebotMessage>>
    fun streamChat(caseId: String, request: QuebotChatRequest): Flow<SseEvent>
    suspend fun getStatus(): Result<QuebotStatus>
}
```

The `streamChat` method returns a `Flow<SseEvent>` instead of a `Result`. This is because SSE is a continuous stream, not a request-response cycle. The Flow emits events as they arrive and completes when the stream ends.

### Fake SSE Stream

```kotlin
class FakeQuebotGateway @Inject constructor() : QuebotGateway {

    override fun streamChat(caseId: String, request: QuebotChatRequest): Flow<SseEvent> = flow {
        emit(SseEvent.Status("thinking"))
        delay(500)

        val response = "This is a simulated response to: ${request.message}"
        for (word in response.split(" ")) {
            emit(SseEvent.Delta("$word "))
            delay(50)
        }

        emit(SseEvent.Done(fullResponse = response, tokensUsed = response.split(" ").size))
    }

    // ... other methods with canned responses
}
```

## Hilt Binding

### Production (default)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {
    @Binds
    abstract fun bindHumanosGateway(impl: HumanosGatewayImpl): HumanosGateway
}

@Module
@InstallIn(SingletonComponent::class)
abstract class QuebotModule {
    @Binds
    abstract fun bindQuebotGateway(impl: QuebotGatewayImpl): QuebotGateway
}
```

### Phase 1 / Offline / Testing

```kotlin
@Module
@InstallIn(SingletonComponent::class)
@TestInstallIn(replaces = [HumanosModule::class])
abstract class FakeHumanosModule {
    @Binds
    abstract fun bindHumanosGateway(fake: FakeHumanosGateway): HumanosGateway
}
```

For Phase 1, the fake modules are used in the `debug` build variant (not just tests) so the app runs without a backend:

```kotlin
// In app/src/debug/kotlin/di/DebugGatewayModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugGatewayModule {
    @Binds
    abstract fun bindHumanosGateway(fake: FakeHumanosGateway): HumanosGateway

    @Binds
    abstract fun bindQuebotGateway(fake: FakeQuebotGateway): QuebotGateway
}
```

## Adding a New Integration

To add a new backend (e.g., a calendar service, a CRM):

1. Create `:integration-newservice` module with the four-layer structure.
2. Define the Retrofit service interface (Layer 1).
3. Define DTOs with `@Serializable` and mappers (Layer 2).
4. Define the Gateway interface with domain model types (Layer 3).
5. Implement `NewServiceGatewayImpl` (real) and `FakeNewServiceGateway` (fake) (Layer 3 + 4).
6. Create Hilt modules for production and debug/test binding.
7. Consume the Gateway interface in `data-*` or `domain-*` modules via `@Inject`.

The feature modules never know or care which backend is behind the Gateway.
