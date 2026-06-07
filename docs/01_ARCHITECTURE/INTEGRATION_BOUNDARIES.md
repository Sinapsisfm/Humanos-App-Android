# Integration Boundaries

> humanOS Native Android -- Backend Integration Rules
> Last updated: 2026-06-06

## Fundamental Rule

The humanOS Android app **NEVER** imports source code from the HumanOS web repo (`humanos-eco`) or the QueBot repo (`ai-core` / QueBot PHP). These are consumed exclusively via their public HTTP APIs. The Android codebase is a fully independent project with its own models, its own Room schema, and its own build pipeline.

## Backend Systems Overview

### HumanOS Web (`humanos-eco`)

| Property | Value |
|---|---|
| Stack | Next.js 15, Prisma (PostgreSQL), NextAuth v5 |
| Endpoints | 450+ REST API routes under `/api/` |
| Auth (web) | NextAuth session cookies + Bearer token (for API calls) |
| Auth (mobile) | Firebase Google Sign-In → bridge JWT (planned, see below) |
| SSE | Used for AI execution streaming in web UI |
| Domains | `www.humanos.eco` (persona), `empresa.eco` (empresa), `estudiante.humanos.eco` |

### QueBot (`ai-core` + QueBot PHP legacy)

| Property | Value |
|---|---|
| Stack | FastAPI (Python), Firebase Auth, Vertex AI, Firestore |
| Endpoints | REST under `/api/v1/` + SSE streaming for chat |
| Auth | Firebase ID token (Bearer) with `tenant_id` claim |
| Legacy | QueBot PHP (FrankenPHP) handles WhatsApp; Android does NOT interact with it |

## Authentication Flow

### Phase 1 (Current -- Mocked)

Auth interfaces are defined but wired to `FakeAuthRepository` that returns canned tokens. No real network calls.

### Phase 2+ (Production)

```
User taps "Sign in with Google"
    |
    v
Firebase Google Sign-In (Android SDK)
    |
    v
Firebase returns: ID token + refresh token
    |
    +──────────────────────────────────────────+
    |                                          |
    v                                          v
QueBot API                              HumanOS API
(direct Firebase ID token)              (bridge JWT exchange)
    |                                          |
    v                                          v
Authorization: Bearer <firebase_id_token>   POST /api/auth/mobile/exchange
                                            Body: { "firebaseToken": "<id_token>" }
                                            Response: { "bridgeJwt": "<jwt>", "expiresIn": 900 }
                                               |
                                               v
                                            Authorization: Bearer <bridge_jwt>
                                            (15-min TTL, auto-refresh)
```

### Bridge JWT Details

The HumanOS backend does not use Firebase Auth natively -- it uses NextAuth v5. A bridge endpoint translates Firebase identity into a HumanOS-compatible JWT.

| Property | Value |
|---|---|
| Endpoint | `POST /api/auth/mobile/exchange` |
| Input | Firebase ID token in request body |
| Validation | Server verifies Firebase token via Firebase Admin SDK |
| Output | HS256 JWT signed with `QUEBOT_BRIDGE_SECRET` |
| Claims | `sub` (user ID), `email`, `name`, `orgId` (if applicable), `iat`, `exp` |
| TTL | 15 minutes |
| Refresh | Re-call the endpoint with a fresh Firebase ID token |

**This endpoint does not exist yet in the HumanOS backend.** It is a planned contract. The Android app codes against the interface, and the mock implementation returns a fake JWT.

### Token Lifecycle

```
App Launch
    |
    v
Check EncryptedSharedPreferences for stored tokens
    |
  found? ──yes──→ Validate expiry
    |                |
    no          expired? ──no──→ Use cached tokens
    |                |
    v           yes
    v                |
Show sign-in    Refresh via Firebase SDK
                     |
                     v
                Exchange for bridge JWT (if HumanOS calls needed)
                     |
                     v
                Store in EncryptedSharedPreferences
```

## Gateway Pattern

Each integration module exposes a Gateway interface. The app module binds the real implementation via Hilt. Tests bind the fake.

### Structure

```
:integration-humanos/
    src/main/
        api/
            HumanosApiService.kt       // Retrofit @GET/@POST interface
        dto/
            TaskDto.kt                  // Network DTOs (kotlinx.serialization)
            UserDto.kt
            ContextSnapshotDto.kt
        gateway/
            HumanosGateway.kt          // Interface (suspend functions)
            HumanosGatewayImpl.kt       // Real implementation using Retrofit
        di/
            HumanosModule.kt           // Hilt @Module providing bindings
    src/test/
        fake/
            FakeHumanosGateway.kt       // Returns canned responses, no network
```

### Gateway Interface Example

```kotlin
interface HumanosGateway {
    suspend fun exchangeToken(firebaseToken: String): Result<BridgeTokenResponse>
    suspend fun getContextSnapshot(since: Instant?): Result<ContextSnapshotDto>
    suspend fun getTasks(page: Int, pageSize: Int): Result<PaginatedResponse<TaskDto>>
    suspend fun createTask(task: CreateTaskRequest): Result<TaskDto>
    suspend fun updateTask(id: String, task: UpdateTaskRequest): Result<TaskDto>
    suspend fun deleteTask(id: String): Result<Unit>
    suspend fun getAppointments(from: Instant, to: Instant): Result<List<AppointmentDto>>
    suspend fun getDailyReview(date: LocalDate): Result<DailyReviewDto>
}
```

### Hilt Binding

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {
    @Binds
    abstract fun bindGateway(impl: HumanosGatewayImpl): HumanosGateway
}

// For tests / Phase 1:
@Module
@InstallIn(SingletonComponent::class)
@TestInstallIn(replaces = [HumanosModule::class])
abstract class FakeHumanosModule {
    @Binds
    abstract fun bindGateway(fake: FakeHumanosGateway): HumanosGateway
}
```

## What the Android App Sees vs. What It Does NOT Touch

### HumanOS

| Consumed (read via API) | Not Touched |
|---|---|
| User profile, session info | Prisma schema (11K lines) |
| Tasks (CRUD) | NextAuth configuration |
| Appointments | Server-side middleware |
| Context snapshot (nodes/edges) | Database migrations |
| Daily review data | Admin routes |
| Health profile summary | Railway deployment config |
| Check-in records | Web-specific UI components |

### QueBot

| Consumed (read via API) | Not Touched |
|---|---|
| Chat SSE streaming | FastAPI application code |
| Case list and status | Firebase project configuration |
| Shopping search results | Vertex AI model configuration |
| | Firestore warm storage |
| | WhatsApp/PHP legacy integration |
| | EventForwarder pipeline |

## Prohibited Dependencies

The following are compile-time violations (enforced by Gradle module structure):

1. **No Prisma client** -- Room is the local database. Models are re-defined in `core-model`.
2. **No NextAuth** -- Firebase Auth is the mobile identity provider. Bridge JWT handles HumanOS API access.
3. **No direct Firestore SDK** -- QueBot data is accessed via REST API, not Firestore client. This avoids pulling in the full Firebase Firestore dependency and its offline cache (which would conflict with Room).
4. **No shared model libraries** -- DTOs in `integration-*` modules are independent Kotlin data classes, not shared with any backend repo.
5. **No backend utility imports** -- No `@humanos/utils`, no Python packages, no PHP includes.

## Error Handling at the Boundary

All gateway calls return `Result<T>`. The repository layer maps failures to user-facing states.

| HTTP Status | Gateway Behavior | Repository Behavior |
|---|---|---|
| 200-299 | `Result.success(parsed body)` | Save to Room, emit success |
| 401 | `Result.failure(UnauthorizedException)` | Trigger token refresh, retry once |
| 403 | `Result.failure(ForbiddenException)` | Emit error, do not retry |
| 404 | `Result.failure(NotFoundException)` | Remove from local cache if exists |
| 429 | `Result.failure(RateLimitException)` | Backoff, retry via WorkManager |
| 500-599 | `Result.failure(ServerException)` | Emit error with cached data, retry later |
| Timeout | `Result.failure(TimeoutException)` | Emit cached data with stale indicator |
| No network | Not called (NetworkMonitor check) | Emit cached data, queue for sync |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Bridge JWT endpoint does not exist yet | Android cannot auth against HumanOS API | Fake gateway in Phase 1; endpoint spec locked in API_CONTRACTS.md |
| HumanOS API changes without notice | Android DTOs break at runtime | Version header on API calls; integration tests against mock server |
| Firebase project ID mismatch (`quebot-2d931` vs `quebot-app`) | Auth tokens rejected by QueBot backend | Verify project ID in Firebase config before release; runtime check on token claims |
| QueBot SSE format changes | Chat streaming breaks | SSE parser handles unknown event types gracefully (logs + ignores) |
| Rate limiting on HumanOS API | Sync fails repeatedly | Exponential backoff in WorkManager with max 6-hour interval |
