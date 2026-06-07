# QueBot -- Read-Only Source Analysis

> humanOS Native Android -- What Was Observed in the QueBot Backend
> Last updated: 2026-06-06
> Source repos: `C:\Users\felip\ai-core\` (FastAPI), QueBot PHP legacy (READ-ONLY, not edited)

## Purpose

This document records what was observed in the QueBot backend codebase that is relevant to the Android app. The Android app does NOT import, share, or depend on any code from these repos. This is a one-way read for contract discovery.

## What Was Observed

### Architecture

| Component | Detail |
|---|---|
| Backend | FastAPI (Python 3), deployed on Railway |
| Auth | Firebase Auth tokens with `tenant_id` claim |
| AI inference | Vertex AI (Google Cloud) for LLM responses |
| Warm storage | Firestore for conversation history and case metadata |
| Streaming | Server-Sent Events (SSE) for real-time chat responses |
| Legacy | QueBot PHP (FrankenPHP) handles WhatsApp integration; Android does NOT interact with this |

### API Endpoints

| Endpoint | Method | Purpose | Android Use |
|---|---|---|---|
| `/api/v1/cases` | GET | List user's cases | Case selector in chat UI |
| `/api/v1/cases` | POST | Create new case | New conversation |
| `/api/v1/cases/{id}` | GET | Get case detail | Case metadata display |
| `/api/v1/cases/{id}/chat` | POST (SSE) | Send message, receive streaming response | Chat screen (primary) |
| `/api/v1/cases/{id}/history` | GET | Get conversation history | Message list on case open |
| `/api/v1/shopping/search` | POST | MercadoLibre product search | Shopping results card in chat |
| `/api/v1/status` | GET | Health check / API status | Connectivity check |
| `/api/v1/models` | GET | Available AI models | Model selector (future) |

### Authentication

| Property | Detail |
|---|---|
| Method | Firebase ID token as Bearer token |
| Header | `Authorization: Bearer <firebase_id_token>` |
| Validation | Server validates via Firebase Admin SDK |
| Tenant | `tenant_id` claim in Firebase token identifies the user's organization |
| Token TTL | 1 hour (Firebase default); auto-refreshed by Firebase SDK |

The Android app uses the Firebase Android SDK to obtain the ID token, then passes it directly to QueBot endpoints. No bridge JWT is needed -- QueBot natively understands Firebase tokens.

### SSE Streaming Protocol

Chat responses are delivered via Server-Sent Events. The client sends a POST request with the prompt, and the server responds with a stream of events.

#### Request Format

```http
POST /api/v1/cases/{case_id}/chat
Authorization: Bearer <firebase_id_token>
Content-Type: application/json

{
  "message": "What is the status of project X?",
  "context": {
    "location": { "lat": -35.42, "lng": -71.65 },
    "recent_captures": ["capture_id_1", "capture_id_2"],
    "energy_score": 0.72
  }
}
```

The `context` object is optional and Android-specific. It provides mobile context signals to improve response relevance.

#### SSE Event Types

| Event Type | Data Format | Description |
|---|---|---|
| `status` | `{"status": "thinking"}` | Agent is processing; show typing indicator |
| `delta` | `{"token": "The"}` | Single token of the response; append to message buffer |
| `search_executed` | `{"query": "...", "result_count": 5, "source": "mercadolibre"}` | Search was performed; show search card in UI |
| `tool_use` | `{"tool": "calculator", "input": "...", "output": "..."}` | Tool was invoked; optionally show tool card |
| `done` | `{"full_response": "...", "tokens_used": 342, "model": "gemini-2.0-flash"}` | Stream complete; finalize message, save to history |
| `error` | `{"code": 500, "message": "Inference failed"}` | Error occurred; show error message, offer retry |

#### SSE Wire Format

```
event: status
data: {"status": "thinking"}

event: delta
data: {"token": "The"}

event: delta
data: {"token": " current"}

event: delta
data: {"token": " status"}

event: search_executed
data: {"query": "project X status", "result_count": 3, "source": "internal"}

event: delta
data: {"token": " is"}

event: delta
data: {"token": " on track."}

event: done
data: {"full_response": "The current status is on track.", "tokens_used": 12, "model": "gemini-2.0-flash"}
```

### Data Models Observed

| Model | Key Fields | Android Mapping |
|---|---|---|
| `Case` | id, user_id, tenant_id, title, status, created_at, updated_at | `QuebotCase` in integration-quebot DTOs |
| `Message` | id, case_id, role (user/assistant), content, created_at | `QuebotMessage` in integration-quebot DTOs |
| `SearchResult` | query, source, results (list), executed_at | Embedded in `SseEvent.SearchExecuted` |

### Firestore Structure (Observed, Not Consumed)

QueBot uses Firestore for warm storage of conversations. The Android app does NOT use the Firestore client SDK directly -- it accesses this data exclusively through the REST API.

```
/tenants/{tenant_id}/cases/{case_id}/
    metadata: { title, status, created_at, updated_at }
    messages/{message_id}: { role, content, created_at }
```

The Android app caches conversation history in its own Room database for offline access and faster UI rendering.

### Error Responses

```json
{
  "error": {
    "code": "INFERENCE_FAILED",
    "message": "The AI model failed to generate a response",
    "details": {
      "model": "gemini-2.0-flash",
      "retry_after_seconds": 30
    }
  }
}
```

| HTTP Status | Error Code | Android Handling |
|---|---|---|
| 400 | `INVALID_REQUEST` | Show validation error, do not retry |
| 401 | `UNAUTHORIZED` | Refresh Firebase token, retry once |
| 403 | `FORBIDDEN` | Show access denied, do not retry |
| 404 | `CASE_NOT_FOUND` | Remove from local cache, show error |
| 429 | `RATE_LIMITED` | Show "please wait", retry after `retry_after_seconds` |
| 500 | `INFERENCE_FAILED` | Show error with retry button |
| 503 | `SERVICE_UNAVAILABLE` | Show maintenance message, retry with backoff |

## What Will NOT Be Touched

1. **FastAPI application code** -- Android consumes the API, does not modify the server.
2. **Firebase project configuration** -- Android uses the Firebase project as-is. The known project ID mismatch (`quebot-2d931` vs `quebot-app`) is documented as a risk.
3. **Vertex AI model configuration** -- Model selection is server-side.
4. **Firestore warm storage** -- Android does NOT use the Firestore SDK. All data comes through REST.
5. **WhatsApp/PHP legacy integration** -- The `EventForwarder` pipeline from QueBot PHP to HumanOS is server-to-server and irrelevant to Android.
6. **Railway deployment configuration** -- QueBot backend is deployed independently.
7. **Python dependencies** -- No Python code runs on Android.

## What Contracts Are Needed

### Already Available (no backend changes needed)

| Endpoint | Status |
|---|---|
| `/api/v1/cases` (GET/POST) | Available |
| `/api/v1/cases/{id}/chat` (POST, SSE) | Available |
| `/api/v1/cases/{id}/history` (GET) | Available |
| `/api/v1/shopping/search` (POST) | Available |
| `/api/v1/status` (GET) | Available |

### Desired Enhancements (not blocking)

| Enhancement | Purpose | Priority |
|---|---|---|
| Mobile context field in chat request | Send location, captures, energy to improve responses | Medium (Phase 2) |
| Push notification trigger on case update | Notify Android when QueBot has async results | Low (Phase 3) |
| Model selection parameter in chat request | Let user choose model from mobile | Low (Phase 3) |

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Firebase project ID mismatch (`quebot-2d931` vs `quebot-app`) | High | Runtime validation of project ID in Firebase config; fail-fast on mismatch with clear error message |
| SSE format changes without notice | Medium | Parser handles unknown event types gracefully (logs + ignores); integration tests against mock SSE server |
| Firestore SDK pulled transitively via Firebase Auth | Low | Explicitly exclude Firestore from Firebase BOM in Gradle; only include `firebase-auth` |
| QueBot rate limiting during heavy chat usage | Low | Client-side rate limiting (max 1 request per 2 seconds); exponential backoff on 429 |
| Vertex AI model deprecation | Low | Model name is server-determined; Android does not hardcode model names |
