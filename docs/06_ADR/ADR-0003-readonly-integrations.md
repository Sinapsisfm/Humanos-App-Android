# ADR-0003: Read-Only Integration Strategy

**Status:** Accepted

**Date:** 2026-06-06

**Deciders:** Felipe Mehr (Sinapsis SpA)

## Context

humanOS Android needs data from two external systems:

### HumanOS (Next.js 15 + Prisma)

- 450+ API endpoints under `/api/`
- Authentication: NextAuth (session-based, cookie-based for web)
- No existing mobile API. All endpoints assume browser session.
- Database: PostgreSQL on Railway
- Real-time: no WebSocket/SSE currently exposed for mobile

### QueBot (PHP 8.2 + FrankenPHP / Next.js v2)

- Authentication: Firebase Auth
- Chat: SSE streaming for agent responses
- Event forwarding: `POST /api/events/ingest` with Bearer token (`QUEBOT_BACKEND_TOKEN`)
- Legacy system with no test coverage -- read-only access is the only safe option

The Android app could:

1. **Full integration**: Build complete API clients with all endpoints, handle auth directly.
2. **Read-only interfaces**: Define gateway interfaces locally, implement real connections later.
3. **BFF (Backend for Frontend)**: Build a dedicated mobile API layer in HumanOS.

## Decision

Integration modules contain **only interfaces, DTOs, and mock implementations**. Real implementations connecting via Retrofit are added when each integration is actively developed (Phase 2+).

### Module Structure

```
integration-humanos/
  src/main/kotlin/.../integration/humanos/
    HumanosGateway.kt            -- interface (public API)
    dto/
      TaskDto.kt                  -- DTO matching HumanOS API response
      BriefingDto.kt
      CaptureDto.kt
      UserProfileDto.kt
    mock/
      MockHumanosGateway.kt      -- fake implementation with static data
    mapper/
      TaskDtoMapper.kt            -- DTO → domain model
    retrofit/                     -- empty until Phase 2
      HumanosApiService.kt       -- Retrofit interface (Phase 2)
      RetrofitHumanosGateway.kt  -- real implementation (Phase 2)
    di/
      HumanosModule.kt           -- binds mock or real based on build config

integration-quebot/
  src/main/kotlin/.../integration/quebot/
    QuebotGateway.kt              -- interface
    dto/
      ChatMessageDto.kt
      AgentResponseDto.kt
    mock/
      MockQuebotGateway.kt
    mapper/
      ChatMessageDtoMapper.kt
    sse/                          -- empty until Phase 2
      QuebotSseClient.kt         -- SSE streaming client (Phase 2)
    di/
      QuebotModule.kt
```

### Gateway Interface Example

```kotlin
interface HumanosGateway {
    
    /** Authenticate with HumanOS and obtain a bridge JWT */
    suspend fun authenticate(firebaseToken: String): AuthResult
    
    /** Fetch tasks for the current user */
    suspend fun getTasks(since: Instant? = null): List<TaskDto>
    
    /** Fetch a single task by ID */
    suspend fun getTask(id: String): TaskDto?
    
    /** Update task status */
    suspend fun updateTaskStatus(id: String, status: String): TaskDto
    
    /** Fetch daily briefing */
    suspend fun getDailyBriefing(date: LocalDate): BriefingDto?
    
    /** Upload a capture */
    suspend fun uploadCapture(capture: CaptureUploadRequest): CaptureDto
    
    /** Fetch user profile */
    suspend fun getUserProfile(): UserProfileDto
    
    /** Sync pending changes (batch) */
    suspend fun syncBatch(changes: SyncBatchRequest): SyncBatchResponse
}
```

### Authentication: Dual-Token Strategy

The Android app must handle two separate authentication systems:

| System | Auth Method | Token Storage |
|---|---|---|
| QueBot | Firebase Auth (ID token) | Firebase SDK manages internally |
| HumanOS | Bridge JWT (exchanged from Firebase token) | Encrypted DataStore |

**Flow:**

```
1. User signs in via Firebase Auth (Google, email, etc.)
2. Firebase SDK provides ID token
3. App sends Firebase ID token to HumanOS:
   POST /api/auth/mobile/exchange
   Body: { "firebaseToken": "<id_token>" }
4. HumanOS validates Firebase token, creates session, returns bridge JWT
5. App stores bridge JWT in encrypted DataStore
6. Subsequent HumanOS API calls use: Authorization: Bearer <bridge_jwt>
7. QueBot API calls use: Authorization: Bearer <firebase_id_token>
```

**Critical dependency**: The endpoint `POST /api/auth/mobile/exchange` does **not exist** in HumanOS today. It must be built in the HumanOS project before real integration works. This is tracked in `docs/03_INTEGRATIONS/HUMANOS_API_SURFACE.md` and `RISKS.md`.

### Build Config Switching

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {
    
    companion object {
        @Provides
        @Singleton
        fun provideHumanosGateway(
            // real: RetrofitHumanosGateway,  // uncomment in Phase 2
            mock: MockHumanosGateway
        ): HumanosGateway {
            return if (BuildConfig.USE_REAL_BACKEND) {
                // real  // uncomment in Phase 2
                mock    // temporary
            } else {
                mock
            }
        }
    }
}
```

In Phase 1, `USE_REAL_BACKEND` is always `false`. Mock implementations return static data that matches the expected API shape.

## Consequences

### What becomes easier

- **Day-one functionality**: The Android app works immediately with mock data. No backend changes required to start building UI and testing flows.
- **Offline development**: Developers can build and test without network access or running HumanOS/QueBot locally.
- **API contract clarity**: Gateway interfaces serve as explicit documentation of what the Android app needs from each backend.
- **Incremental integration**: Each endpoint can be connected to the real backend independently, one at a time.
- **Testing**: Mock implementations are the default test doubles. No special test setup needed.

### What becomes harder

- **Mock data maintenance**: Mock implementations must be kept realistic as the real API evolves.
- **Late integration risk**: Real API issues (auth, rate limits, error formats, pagination) are not discovered until Phase 2.
- **Dual auth complexity**: Managing two token types, two refresh cycles, and two failure modes adds complexity.
- **Missing backend endpoint**: The `POST /api/auth/mobile/exchange` endpoint is a hard blocker for real integration. If it is not built in HumanOS, the Android app cannot authenticate.

### Mitigations

- Integration tests (Phase 2) will run against a staging HumanOS instance to catch API drift early.
- The mock implementations are structured to match real API response shapes as closely as possible.
- Auth complexity is isolated in `core-security` and `core-network` modules.
- The missing endpoint is tracked as a critical risk (RISK-003 in RISKS.md) and as a task dependency.

## References

- DEC-004: Integration approach decision
- DEC-005: Dual-token authentication strategy
- ADR-0001: Project separation rationale
- `docs/03_INTEGRATIONS/HUMANOS_API_SURFACE.md`: Full API contract
- `docs/03_INTEGRATIONS/QUEBOT_INTEGRATION.md`: QueBot integration details
- `docs/03_INTEGRATIONS/AUTH_STRATEGY.md`: Authentication flow details
