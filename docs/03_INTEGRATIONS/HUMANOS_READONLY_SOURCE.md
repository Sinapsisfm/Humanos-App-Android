# HumanOS -- Read-Only Source Analysis

> humanOS Native Android -- What Was Observed in the HumanOS Web Codebase
> Last updated: 2026-06-06
> Source repo: `C:\Users\felip\humanos-eco\` (READ-ONLY reference)

## Purpose

This document records what was observed in the HumanOS web codebase (`humanos-eco`) that is relevant to the Android app. The Android app does NOT import, share, or depend on any code from this repo. This is a one-way read for contract discovery.

## What Was Observed

### API Surface

HumanOS exposes 450+ API routes under `/api/`. These are Next.js API route handlers (App Router, `route.ts` files). The Android app will consume a subset of these via REST.

Key route groups relevant to Android:

| Route Group | Example Endpoints | Android Use |
|---|---|---|
| `/api/auth/` | Session management, providers | Mobile auth bridge (new endpoint needed) |
| `/api/tasks/` | CRUD, status updates, assignment | Task sync |
| `/api/appointments/` | CRUD, calendar integration | Appointment display |
| `/api/context/` | Snapshot, node CRUD, edge CRUD | Context graph sync |
| `/api/captures/` | Create, list, media upload | Capture sync |
| `/api/health/` | Profile, check-ins, medications | Health data display |
| `/api/daily-review/` | Generate, get, complete | Daily review screen |
| `/api/persons/` | CRUD, relationships | Contact/person sync |
| `/api/ai/` | Execution, status | AI execution triggers |
| `/api/events/ingest` | Event ingestion from QueBot | Not used by Android directly |

### Authentication Architecture

| Component | Detail |
|---|---|
| Library | NextAuth v5 (`@auth/nextjs`) |
| Session strategy | JWT (server-side) + session cookie (browser) |
| Providers | Google OAuth, credentials (email/password) |
| Bearer token support | Yes -- `Authorization: Bearer <token>` validated in middleware |
| Mobile support | **Not yet implemented** -- requires bridge endpoint |

#### Identity Bridge JWT (Observed Contract)

The HumanOS codebase contains a bridge JWT signing mechanism used for QueBot-to-HumanOS communication:

| Property | Observed Value |
|---|---|
| Algorithm | HS256 |
| Secret | `QUEBOT_BRIDGE_SECRET` environment variable |
| TTL | 15 minutes (900 seconds) |
| Claims | `sub` (user ID), `email`, `name`, `orgId`, `iat`, `exp` |
| Audience | HumanOS API |

The Android app will need a new endpoint (`POST /api/auth/mobile/exchange`) that accepts a Firebase ID token and returns a bridge JWT with the same signing scheme. This endpoint does not exist yet.

### Database Schema (Prisma)

The Prisma schema is approximately 11,000 lines covering 80+ models. Key models relevant to Android:

| Model | Key Fields | Android Mapping |
|---|---|---|
| `User` | id, email, name, image, role, orgId | `User` in core-model |
| `Person` | id, userId, name, email, phone, role, relationship | `Person` in core-model |
| `Task` | id, title, description, status, priority, dueDate, assigneeId, projectId | `Task` in core-model |
| `Note` | id, content, type, personId, captureSource | Maps to `Capture` in core-model |
| `Appointment` | id, title, startTime, endTime, status, locationId, attendees | `Appointment` in core-model |
| `HealthProfile` | id, personId, bloodType, allergies, conditions, medications | `HealthProfile` in core-model |
| `CheckIn` | id, personId, mood, energy, notes, timestamp | `CheckIn` in core-model |
| `AiExecution` | id, model, prompt, response, tokensUsed, cost, status | Not directly mapped; Android uses QueBot for AI |
| `Organization` | id, name, taxId, industry, ownerId | `Organization` in core-model |
| `ActivityEvent` | id, type, payload, source, timestamp | `TraceEvent` analogue in core-model |

### Middleware and Security

| Aspect | Observed |
|---|---|
| CORS | Configured for `humanos.eco`, `empresa.eco`, `estudiante.humanos.eco` |
| Rate limiting | Not observed at application level (may be at Railway/infra) |
| CSRF | NextAuth built-in CSRF tokens (cookie-based, not relevant for API) |
| Multi-tenant | `orgId` injected via middleware for `empresa.eco` host |
| Privacy levels | `PrivacyLevel` enum in Prisma: `PUBLIC`, `PRIVATE`, `VAULT` |

### Data Formats

| Format | Detail |
|---|---|
| Date/time | ISO 8601 strings in JSON responses |
| IDs | UUID v4 (cuid in some older models) |
| Pagination | `{ data: T[], total: number, page: number, pageSize: number }` |
| Errors | `{ error: string, code?: string, details?: object }` |
| Enums | Uppercase snake_case strings (e.g., `IN_PROGRESS`, `HIGH_PRIORITY`) |

## What Will NOT Be Touched

The following are explicitly out of scope for the Android project:

1. **Prisma schema** -- The Android app defines its own Room entities. No shared model library.
2. **NextAuth configuration** -- Android uses Firebase Auth, not NextAuth.
3. **Server-side middleware** -- Android does not run Next.js middleware.
4. **Admin routes** (`/api/admin/*`) -- Android is a user-facing client, not an admin tool.
5. **Web UI components** -- No React/Compose interop. Completely separate UI.
6. **Database migrations** -- Room has its own migration system.
7. **Railway deployment configuration** -- Android deploys via Play Store, not Railway.
8. **Environment variables** -- Android uses `BuildConfig` and `local.properties`, not `.env`.
9. **QueBot event forwarding pipeline** (`/api/events/ingest`) -- This is server-to-server.
10. **Cron jobs** -- Android uses WorkManager for background tasks.

## What Contracts/APIs Are Needed

### Required from HumanOS Backend (not yet built)

| Endpoint | Method | Purpose | Priority |
|---|---|---|---|
| `/api/auth/mobile/exchange` | POST | Accept Firebase ID token, return bridge JWT | **Critical** (blocks Phase 2 auth) |
| `/api/context/snapshot` | GET | Return changed nodes/edges since timestamp | High (Phase 2 sync) |
| `/api/context/batch` | POST | Accept batch of locally-created nodes/edges | High (Phase 2 sync) |
| `/api/sync/status` | GET | Return sync watermark and conflict count | Medium (Phase 2 sync) |

### Existing Endpoints to Consume (already built)

| Endpoint | Method | Android Use |
|---|---|---|
| `/api/tasks` | GET | List tasks with pagination |
| `/api/tasks` | POST | Create task |
| `/api/tasks/[id]` | PUT | Update task |
| `/api/tasks/[id]` | DELETE | Soft-delete task |
| `/api/appointments` | GET | List appointments in date range |
| `/api/persons` | GET | List persons for context |
| `/api/health/profile` | GET | Get user health profile |
| `/api/health/check-ins` | GET/POST | Get/create check-ins |
| `/api/daily-review/[date]` | GET | Get daily review for date |
| `/api/daily-review/generate` | POST | Trigger AI daily review generation |

## Prohibited Dependencies

The Android project must NEVER add:

- `@prisma/client` or any Prisma-related package
- `next-auth` or `@auth/core`
- Any npm package from the HumanOS `package.json`
- Direct database connection strings to the HumanOS PostgreSQL instance
- Server-side environment variables from the HumanOS deployment

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Bridge JWT endpoint not yet built | High | Android uses FakeHumanosGateway in Phase 1; endpoint spec locked here |
| HumanOS API changes break Android DTOs | Medium | Version header on requests; integration tests against mock server; DTO mapping layer absorbs changes |
| Pagination format differs per endpoint | Low | Observed consistent pattern; verify per endpoint during Phase 2 integration |
| Privacy level enforcement differs between web and Android | Medium | Android re-implements PrivacyLevel logic locally; sync excludes VAULT items at repository layer |
| Rate limiting applied at infra level without documented limits | Low | Exponential backoff on 429; conservative sync frequency (15-min minimum) |
