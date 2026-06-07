# API Contracts

> humanOS Native Android -- Expected DTOs and Endpoint Contracts
> Last updated: 2026-06-06

## Overview

This document defines the exact request/response contracts the Android app expects from each backend. These contracts serve as the interface specification for both the real Gateway implementations and the fake/mock implementations used in Phase 1 and testing.

---

## HumanOS API Contracts

Base URL: `https://www.humanos.eco/api`
Auth: `Authorization: Bearer <bridge_jwt>`

### 1. Auth Exchange

Exchanges a Firebase ID token for a HumanOS bridge JWT.

**Endpoint**: `POST /api/auth/mobile/exchange`
**Status**: NOT YET BUILT -- this is the planned contract.

#### Request

```kotlin
@Serializable
data class MobileAuthExchangeRequest(
    val firebaseToken: String,
    val deviceId: String,       // Android device ID for session tracking
    val platform: String = "android",
)
```

```http
POST /api/auth/mobile/exchange
Content-Type: application/json

{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "deviceId": "a1b2c3d4-e5f6-7890",
  "platform": "android"
}
```

#### Response (200)

```kotlin
@Serializable
data class MobileAuthExchangeResponse(
    val bridgeJwt: String,
    val expiresIn: Int,         // seconds until expiry (900 = 15 min)
    val userId: String,
    val email: String,
    val displayName: String,
    val orgId: String?,         // null if personal account
)
```

```json
{
  "bridgeJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "expiresIn": 900,
  "userId": "clx1abc123",
  "email": "user@example.com",
  "displayName": "Felipe Mehr",
  "orgId": null
}
```

#### Error (401)

```json
{
  "error": "Invalid or expired Firebase token",
  "code": "INVALID_FIREBASE_TOKEN"
}
```

---

### 2. Context Snapshot

Returns all context nodes and edges modified since a given timestamp.

**Endpoint**: `GET /api/context/snapshot`

#### Request

```http
GET /api/context/snapshot?since=2026-06-01T00:00:00Z&limit=500
Authorization: Bearer <bridge_jwt>
```

Query parameters:
- `since` (optional): ISO 8601 timestamp. Returns only items modified after this time. Omit for full snapshot.
- `limit` (optional): Max items per page. Default 500.
- `cursor` (optional): Pagination cursor from previous response.

#### Response (200)

```kotlin
@Serializable
data class ContextSnapshotResponse(
    val nodes: List<ContextNodeDto>,
    val edges: List<ContextEdgeDto>,
    val cursor: String?,        // null if no more pages
    val serverTimestamp: String, // ISO 8601, use as next `since` value
)

@Serializable
data class ContextNodeDto(
    val id: String,
    val type: String,           // "PERSON", "TASK", etc.
    val label: String,
    val summary: String?,
    val metadata: Map<String, String>,
    val governanceState: String,
    val privacyLevel: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
data class ContextEdgeDto(
    val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationshipType: String,
    val weight: Float,
    val governanceState: String,
    val createdAt: String,
    val updatedAt: String,
)
```

---

### 3. Context Batch Push

Sends locally-created or modified nodes and edges to the server.

**Endpoint**: `POST /api/context/batch`

#### Request

```kotlin
@Serializable
data class ContextBatchRequest(
    val nodes: List<ContextNodeDto>,
    val edges: List<ContextEdgeDto>,
    val clientTimestamp: String,
)
```

#### Response (200)

```kotlin
@Serializable
data class ContextBatchResponse(
    val accepted: Int,
    val rejected: Int,
    val conflicts: List<ConflictDto>,
    val serverTimestamp: String,
)

@Serializable
data class ConflictDto(
    val entityType: String,     // "node" or "edge"
    val entityId: String,
    val reason: String,         // "VERSION_CONFLICT", "DELETED_ON_SERVER"
    val serverVersion: String?, // JSON of server's current version
)
```

---

### 4. Tasks CRUD

**List Tasks**: `GET /api/tasks`

```http
GET /api/tasks?page=1&pageSize=20&status=IN_PROGRESS,TODO&sortBy=dueDate
Authorization: Bearer <bridge_jwt>
```

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,         // "TODO", "IN_PROGRESS", "DONE", "CANCELLED"
    val priority: String,       // "LOW", "MEDIUM", "HIGH", "URGENT"
    val dueDate: String?,       // ISO 8601 date
    val assigneeId: String?,
    val projectId: String?,
    val contextNodeIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
)
```

**Create Task**: `POST /api/tasks`

```kotlin
@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val assigneeId: String? = null,
    val projectId: String? = null,
    val contextNodeIds: List<String> = emptyList(),
)
```

**Update Task**: `PUT /api/tasks/{id}`

```kotlin
@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
    val assigneeId: String? = null,
    val projectId: String? = null,
    val contextNodeIds: List<String>? = null,
)
```

**Delete Task**: `DELETE /api/tasks/{id}` -- Returns 204 No Content (soft delete on server).

---

### 5. Appointments

**List Appointments**: `GET /api/appointments`

```http
GET /api/appointments?from=2026-06-01T00:00:00Z&to=2026-06-30T23:59:59Z
Authorization: Bearer <bridge_jwt>
```

```kotlin
@Serializable
data class AppointmentDto(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: String,      // ISO 8601
    val endTime: String,        // ISO 8601
    val status: String,         // "SCHEDULED", "CONFIRMED", "CANCELLED", "COMPLETED"
    val locationName: String?,
    val locationAddress: String?,
    val attendeeIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
)
```

---

### 6. Daily Review

**Get Daily Review**: `GET /api/daily-review/{date}`

```http
GET /api/daily-review/2026-06-06
Authorization: Bearer <bridge_jwt>
```

```kotlin
@Serializable
data class DailyReviewDto(
    val date: String,           // ISO 8601 date
    val summary: String?,       // AI-generated summary text
    val tasksCompleted: Int,
    val tasksCreated: Int,
    val tasksDue: List<TaskDto>,
    val appointmentsToday: List<AppointmentDto>,
    val captureCount: Int,
    val checkIn: CheckInDto?,
    val suggestions: List<String>,
    val generatedAt: String?,   // null if not yet generated
)

@Serializable
data class CheckInDto(
    val id: String,
    val mood: Int,              // 1-5 scale
    val energy: Int,            // 1-5 scale
    val notes: String?,
    val timestamp: String,
)
```

**Generate Daily Review**: `POST /api/daily-review/generate`

```kotlin
@Serializable
data class GenerateDailyReviewRequest(
    val date: String,
    val includeAiSummary: Boolean = true,
)
```

Returns 202 Accepted with a `taskId` for polling, or 200 with the completed review if generation is fast.

---

## QueBot API Contracts

Base URL: Configured per environment (e.g., `https://quebot-api.railway.app/api/v1`)
Auth: `Authorization: Bearer <firebase_id_token>`

### 1. Chat (SSE Streaming)

**Endpoint**: `POST /api/v1/cases/{case_id}/chat`

#### Request

```kotlin
@Serializable
data class QuebotChatRequest(
    val message: String,
    val context: QuebotMobileContext? = null,
)

@Serializable
data class QuebotMobileContext(
    val location: LocationDto? = null,
    val recentCaptureIds: List<String> = emptyList(),
    val energyScore: Float? = null,
    val activeProjectId: String? = null,
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
)
```

#### Response (SSE stream)

See SSE event types in `QUEBOT_READONLY_SOURCE.md`. The Kotlin sealed class:

```kotlin
@Serializable
sealed class SseEventDto {
    @Serializable
    @SerialName("status")
    data class Status(val status: String) : SseEventDto()

    @Serializable
    @SerialName("delta")
    data class Delta(val token: String) : SseEventDto()

    @Serializable
    @SerialName("search_executed")
    data class SearchExecuted(
        val query: String,
        val resultCount: Int,
        val source: String,
    ) : SseEventDto()

    @Serializable
    @SerialName("tool_use")
    data class ToolUse(
        val tool: String,
        val input: String,
        val output: String,
    ) : SseEventDto()

    @Serializable
    @SerialName("done")
    data class Done(
        val fullResponse: String,
        val tokensUsed: Int,
        val model: String,
    ) : SseEventDto()

    @Serializable
    @SerialName("error")
    data class Error(
        val code: Int,
        val message: String,
    ) : SseEventDto()
}
```

---

### 2. Case Management

**List Cases**: `GET /api/v1/cases`

```kotlin
@Serializable
data class QuebotCaseDto(
    val id: String,
    val title: String,
    val status: String,         // "active", "archived"
    val messageCount: Int,
    val createdAt: String,
    val updatedAt: String,
)
```

**Create Case**: `POST /api/v1/cases`

```kotlin
@Serializable
data class CreateCaseRequest(
    val title: String,
)
```

**Get Case History**: `GET /api/v1/cases/{case_id}/history`

```kotlin
@Serializable
data class QuebotMessageDto(
    val id: String,
    val caseId: String,
    val role: String,           // "user", "assistant"
    val content: String,
    val createdAt: String,
)

// Response is a list of QuebotMessageDto
```

---

### 3. Status Check

**Endpoint**: `GET /api/v1/status`

```kotlin
@Serializable
data class QuebotStatusResponse(
    val status: String,         // "ok", "degraded", "maintenance"
    val version: String,
    val models: List<String>,
    val timestamp: String,
)
```

Used by the Android app on launch to verify QueBot connectivity before showing the chat option.

---

## DTO-to-Model Mapping Convention

DTOs (in `integration-*` modules) map to domain models (in `core-model`) via extension functions:

```kotlin
// In integration-humanos
fun TaskDto.toTask(): Task = Task(
    id = id,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    dueDate = dueDate?.let { LocalDate.parse(it) },
    // ...
)

fun Task.toDto(): TaskDto = TaskDto(
    id = id,
    title = title,
    description = description,
    status = status.name,
    priority = priority.name,
    dueDate = dueDate?.toString(),
    // ...
)
```

DTOs use `String` for enums and dates. Domain models use proper Kotlin types. The mapping layer absorbs any backend format changes.

## Versioning Strategy

All requests include a custom header:

```http
X-HumanOS-Client: android/1.0.0
X-HumanOS-API-Version: 2026-06-01
```

The `X-HumanOS-API-Version` header signals which contract version the client expects. The server can use this to maintain backward compatibility or return a 406 if the version is no longer supported.
