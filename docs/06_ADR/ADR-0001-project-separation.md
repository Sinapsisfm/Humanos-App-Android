# ADR-0001: Project Separation from HumanOS and QueBot

**Status:** Accepted

**Date:** 2026-06-06

**Deciders:** Felipe Mehr (Sinapsis SpA)

## Context

humanOS Android is a native mobile client for the humanOS ecosystem. The ecosystem currently includes:

- **HumanOS** (`humanos-eco`): Next.js 15 + Prisma web application with 450+ API endpoints, NextAuth authentication, deployed on Railway. Serves `www.humanos.eco`, `empresa.eco`, and `estudiante.humanos.eco`.
- **QueBot** (`quebot`): Legacy PHP 8.2 + FrankenPHP chatbot with Firebase Auth, WhatsApp integration, SSE streaming. Deployed on Railway.
- **QueBot v2** (`quebot-v2`): Next.js 15 rebuild of QueBot, in progress.
- **AI Core** (`ai-core`): Python 3 + FastAPI backend for legal, SII, PJUD, and scraper services.

The Android app needs data from HumanOS (tasks, briefings, terrain, health) and QueBot (chat, agent interactions). There are three possible approaches:

1. **Monorepo**: Share code with HumanOS web via a shared TypeScript/Kotlin bridge.
2. **Shared libraries**: Extract common logic into shared packages consumed by both web and mobile.
3. **Complete separation**: Independent project with explicit integration contracts.

## Decision

humanOS Android is a **completely independent project**. It does not share code, dependencies, build systems, or repositories with HumanOS, QueBot, or AI Core.

- HumanOS and QueBot are treated as **external, read-only data sources**.
- All integrations happen through **gateway interfaces** defined in the Android project.
- Integration modules (`integration-humanos`, `integration-quebot`) contain only interfaces, DTOs, and mock implementations.
- Real implementations connect via Retrofit HTTP clients.
- No import of TypeScript, PHP, or Python code. No transpilation. No shared schemas.

## Consequences

### What becomes easier

- **Independent release cycle**: Android app can ship on its own schedule, unblocked by web deploys.
- **Clean dependency graph**: No transitive dependency on Next.js, Prisma, or PHP packages.
- **Testability**: Mock integrations from day one. The app works offline with fake data before any backend changes.
- **Team scalability**: An Android developer does not need to understand the HumanOS or QueBot codebase.
- **Technology freedom**: Android-native choices (Kotlin, Compose, Hilt, Room) without compromise.

### What becomes harder

- **No code reuse**: Domain models, validation logic, and business rules must be re-implemented in Kotlin. Drift between web and mobile domain models is possible.
- **API contract maintenance**: Changes to HumanOS or QueBot APIs require manual updates to Android DTOs and mappers. There is no shared schema enforcement.
- **Dual authentication**: The Android app must handle two separate auth systems (Firebase for QueBot, bridge JWT for HumanOS). A new endpoint `POST /api/auth/mobile/exchange` is required in HumanOS -- but that endpoint is NOT built as part of this project.
- **Feature parity tracking**: Features available on web but not on mobile (and vice versa) must be explicitly tracked.

### Mitigations

- Integration contracts are documented in `docs/03_INTEGRATIONS/`.
- API changes are detected during integration testing (Phase 2+).
- Domain model drift is tracked in RISKS.md.
- The `integration-*` modules serve as the single point of change when backend APIs evolve.

## References

- DEC-001: Project scope and separation rationale
- `docs/03_INTEGRATIONS/HUMANOS_API_SURFACE.md`: HumanOS API contract
- `docs/03_INTEGRATIONS/QUEBOT_INTEGRATION.md`: QueBot integration contract
