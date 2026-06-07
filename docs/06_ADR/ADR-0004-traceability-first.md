# ADR-0004: Traceability as a Product Feature

**Status:** Accepted

**Date:** 2026-06-06

**Deciders:** Felipe Mehr (Sinapsis SpA)

## Context

Documentation and traceability in software projects tend to rot. README files go stale within weeks. Architecture diagrams diverge from reality. Decision rationale is lost in Slack threads. When something breaks, nobody knows why a particular choice was made or when it changed.

This is not just a process problem -- in the humanOS ecosystem, traceability has regulatory and operational implications:

- **Health data** (Phase 3) requires audit trails under Chilean privacy law (Ley 21.719).
- **Agent actions** must be traceable to understand what the AI did and why.
- **Field inspections** and captures must have provenance (who, when, where, what device).
- **Multi-system integration** (HumanOS + QueBot + Android) creates data lineage questions.

Two approaches were considered:

1. **Administrative traceability**: Maintain docs as an afterthought. Periodically audit and update. Accept some drift.
2. **Traceability as a product feature**: Build traceability into the application architecture. Every significant event, decision, and data change is recorded systematically.

## Decision

Traceability is a **product feature**, not administrative overhead. It is built into the architecture at multiple levels:

### Level 1: Project Documentation Traceability

Every significant artifact has a canonical location and is cross-referenced:

| Artifact | Location | Updated When |
|---|---|---|
| Module inventory | `docs/01_ARCHITECTURE/MODULE_MAP.md` | Module added/removed/renamed |
| Decisions | `docs/00_PROJECT_CONTROL/DECISIONS_LOG.md` | Decision made |
| Architectural decisions | `docs/06_ADR/ADR-NNNN-*.md` | Major architecture choice |
| Risks | `docs/00_PROJECT_CONTROL/RISKS.md` | Risk identified/mitigated/escalated |
| Tasks | `docs/00_PROJECT_CONTROL/TASKS.md` | Task created/started/completed |
| Current state | `docs/00_PROJECT_CONTROL/CURRENT_STATE.md` | Any significant change |
| API contracts | `docs/03_INTEGRATIONS/*.md` | Backend API changes |

**Rule**: If it is not in one of these documents, it does not exist as a project fact. Verbal agreements, Slack messages, and PR comments are not authoritative.

### Level 2: Code Traceability

Every code artifact references its documentation:

- Commit messages reference tasks: `Refs: TASK-XXX`
- Commit messages reference decisions: `Refs: DEC-XXX`
- TODO comments reference tasks: `// TODO(TASK-XXX): description`
- Module `build.gradle.kts` comments reference MODULE_MAP entry
- Integration DTOs reference API contract document

### Level 3: In-App Traceability (Runtime)

The application itself records an audit trail of significant events:

#### TraceEvent Model

```kotlin
@Entity(tableName = "trace_events")
data class TraceEvent(
    @PrimaryKey val id: String,                // UUID
    val timestamp: Instant,                     // when it happened
    val category: TraceCategory,                // what kind of event
    val action: String,                         // what happened (verb)
    val subject: String,                        // what was affected (noun)
    val subjectId: String?,                     // ID of the affected entity
    val actor: TraceActor,                      // who/what did it
    val details: String?,                       // JSON blob with additional context
    val source: TraceSource,                    // where the event originated
    val syncedToServer: Boolean = false         // whether it has been synced
)

enum class TraceCategory {
    AUTH,           // login, logout, token refresh
    CAPTURE,        // photo taken, audio recorded, document scanned
    TASK,           // task created, updated, completed
    SYNC,           // sync started, completed, conflict resolved
    AGENT,          // agent query, response, action taken
    HEALTH,         // health data imported, reading recorded
    SETTINGS,       // user changed a setting
    ERROR,          // unhandled error, crash
    NAVIGATION      // screen visited (opt-in, privacy-sensitive)
}

enum class TraceActor {
    USER,           // human user action
    AGENT,          // AI agent action
    SYSTEM,         // automated system action (sync, worker)
    EXTERNAL        // external system event (push notification, webhook)
}

enum class TraceSource {
    ANDROID_APP,    // this app
    HUMANOS_API,    // received from HumanOS
    QUEBOT_API,     // received from QueBot
    HEALTH_CONNECT, // imported from Health Connect
    FIREBASE        // Firebase event
}
```

#### SourceReference Model

```kotlin
/**
 * Tracks the origin of any piece of data in the app.
 * Attached to entities that may come from multiple sources.
 */
@Entity(tableName = "source_references")
data class SourceReference(
    @PrimaryKey val id: String,                // UUID
    val entityType: String,                     // "Task", "Capture", "EnergySignal"
    val entityId: String,                       // ID of the entity
    val source: TraceSource,                    // where it came from
    val sourceId: String?,                      // ID in the source system
    val sourceTimestamp: Instant?,              // timestamp in the source system
    val importedAt: Instant,                    // when humanOS Android ingested it
    val lastSyncedAt: Instant?,                // last successful sync
    val syncStatus: SyncStatus                  // current sync state
)

enum class SyncStatus {
    LOCAL_ONLY,         // created locally, not yet synced
    SYNCED,             // in sync with server
    LOCAL_MODIFIED,     // modified locally since last sync
    SERVER_MODIFIED,    // server has newer version
    CONFLICT,           // both modified, needs resolution
    SYNC_ERROR          // last sync attempt failed
}
```

#### Usage Example

```kotlin
class CaptureRepositoryImpl @Inject constructor(
    private val captureDao: CaptureDao,
    private val traceEventDao: TraceEventDao,
    private val sourceReferenceDao: SourceReferenceDao
) : CaptureRepository {

    override suspend fun createCapture(capture: CaptureItem): CaptureItem {
        captureDao.insert(capture.toEntity())
        
        // Record the trace event
        traceEventDao.insert(TraceEvent(
            id = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            category = TraceCategory.CAPTURE,
            action = "created",
            subject = "CaptureItem",
            subjectId = capture.id,
            actor = TraceActor.USER,
            details = """{"type": "${capture.type}", "hasLocation": ${capture.location != null}}""",
            source = TraceSource.ANDROID_APP
        ))
        
        // Record the source reference
        sourceReferenceDao.insert(SourceReference(
            id = UUID.randomUUID().toString(),
            entityType = "CaptureItem",
            entityId = capture.id,
            source = TraceSource.ANDROID_APP,
            sourceId = null,
            sourceTimestamp = null,
            importedAt = Instant.now(),
            lastSyncedAt = null,
            syncStatus = SyncStatus.LOCAL_ONLY
        ))
        
        return capture
    }
}
```

## Consequences

### What becomes easier

- **Debugging**: When something goes wrong, the trace log shows exactly what happened, when, and who initiated it.
- **Audit compliance**: Health data provenance is built in, not bolted on. Ley 21.719 audit requirements are met by architecture.
- **Sync debugging**: SourceReference makes it clear which system owns each piece of data and whether it is in sync.
- **Agent accountability**: Every AI agent action is recorded. Users and operators can review what the agent did.
- **Onboarding**: New team members can trace any feature from documentation to code to runtime behavior.
- **Post-mortem**: After incidents, the trace log provides a timeline without relying on memory or log file archaeology.

### What becomes harder

- **Development velocity**: Every repository method that creates or modifies data must also record a TraceEvent and manage SourceReference. This is more code per operation.
- **Storage growth**: TraceEvent and SourceReference tables grow continuously. Requires periodic pruning strategy.
- **Performance**: Extra database writes per operation. Mitigated by batching and WAL mode in Room.
- **Discipline**: The traceability system is only valuable if it is consistently used. One un-traced operation breaks the audit chain.

### Mitigations

- **Base repository class**: A `TracedRepository` base class can automate TraceEvent creation for common CRUD operations.
- **Pruning worker**: A WorkManager task prunes TraceEvents older than 90 days (configurable). SourceReferences are kept as long as the entity exists.
- **CI enforcement**: A custom detekt rule can warn when a repository method does not record a TraceEvent.
- **Documentation checks**: Pre-commit or CI step verifies that new modules appear in MODULE_MAP.md and new decisions appear in DECISIONS_LOG.md.

## References

- DEC-006: Traceability approach
- `docs/00_PROJECT_CONTROL/DECISIONS_LOG.md`: Decision log
- `docs/00_PROJECT_CONTROL/RISKS.md`: Risk registry
- `docs/00_PROJECT_CONTROL/TASKS.md`: Task registry
