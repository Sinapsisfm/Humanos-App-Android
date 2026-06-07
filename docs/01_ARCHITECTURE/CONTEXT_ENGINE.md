# Context Engine

> humanOS Native Android -- Context Graph Architecture
> Last updated: 2026-06-06

## Overview

The Context Engine is the conceptual core of humanOS. It models the user's world as a graph of interconnected nodes (people, places, projects, events, tasks, captures, decisions, organizations, documents, medications) linked by typed, weighted edges. The goal is to make the phone understand context the way a human assistant would -- by remembering relationships, inferring connections, and surfacing relevant information at the right moment.

## Phase Rollout

| Phase | Scope |
|---|---|
| Phase 1 (current) | Data models defined in `core-model`. Kotlin data classes and enums. No storage, no logic. |
| Phase 2 | Room storage (`context_nodes`, `context_edges` tables). CRUD use cases in `domain-context`. Sync with HumanOS backend. Manual link creation UI. |
| Phase 3 | Semantic search via on-device embeddings. AI-inferred edges. Automatic context attachment to captures. Proactive surfacing in Daily Review. |

## Node Model

```kotlin
data class ContextNode(
    val id: String,                    // UUID v4
    val type: ContextNodeType,
    val label: String,                 // Human-readable name
    val summary: String?,              // AI-generated or user-written
    val metadata: Map<String, String>, // Type-specific key-value pairs
    val governanceState: GovernanceState,
    val privacyLevel: PrivacyLevel,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,           // Soft delete
    val sourceId: String?,             // Origin capture/event ID
    val syncState: SyncState,
)
```

### Node Types

```kotlin
enum class ContextNodeType {
    PERSON,         // Contacts, family, colleagues, patients
    PLACE,          // Addresses, offices, hospitals, landmarks
    PROJECT,        // Work projects, personal goals, courses
    EVENT,          // Calendar events, appointments, meetings
    TASK,           // Action items, todos, delegated work
    CAPTURE,        // Raw captures (text, voice, photo, link)
    DECISION,       // Recorded decisions with rationale
    ORGANIZATION,   // Companies, institutions, teams
    DOCUMENT,       // Files, reports, contracts, prescriptions
    MEDICATION,     // Drugs, dosages, schedules, interactions
}
```

### Metadata Examples by Type

| Type | Metadata Keys |
|---|---|
| PERSON | `email`, `phone`, `role`, `organization_id`, `relationship` |
| PLACE | `latitude`, `longitude`, `address`, `place_type` (home/work/clinic) |
| PROJECT | `status` (active/paused/done), `deadline`, `owner_id` |
| EVENT | `start_time`, `end_time`, `location_id`, `recurrence` |
| TASK | `priority`, `due_date`, `assignee_id`, `project_id`, `status` |
| CAPTURE | `capture_type` (text/voice/photo/link), `media_uri`, `transcription` |
| DECISION | `rationale`, `alternatives`, `decided_by`, `decided_at` |
| ORGANIZATION | `industry`, `website`, `tax_id`, `parent_org_id` |
| DOCUMENT | `file_type`, `file_uri`, `page_count`, `signed_by` |
| MEDICATION | `drug_name`, `dosage`, `frequency`, `prescriber_id`, `active` |

## Edge Model

```kotlin
data class ContextEdge(
    val id: String,                     // UUID v4
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationshipType: RelationshipType,
    val weight: Float,                  // 0.0 to 1.0, decays over time
    val governanceState: GovernanceState,
    val metadata: Map<String, String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastReinforcedAt: Instant?,     // Reset on user interaction
)
```

### Relationship Types

```kotlin
enum class RelationshipType {
    // Person relationships
    WORKS_WITH,
    REPORTS_TO,
    FAMILY_OF,
    PATIENT_OF,
    TREATS,                // Doctor → Patient

    // Organizational
    MEMBER_OF,
    OWNS,
    LOCATED_AT,

    // Project/Task
    ASSIGNED_TO,
    PART_OF,               // Task → Project, SubProject → Project
    BLOCKED_BY,
    DEPENDS_ON,

    // Event
    ATTENDEE_OF,
    SCHEDULED_AT,          // Event → Place

    // Capture/Document
    ABOUT,                 // Capture → any node it references
    ATTACHED_TO,
    MENTIONED_IN,

    // Decision
    DECIDED_FOR,           // Decision → chosen option node
    DECIDED_AGAINST,

    // Medical
    PRESCRIBED_TO,         // Medication → Person
    PRESCRIBED_BY,         // Medication → Person (doctor)
    INTERACTS_WITH,        // Medication → Medication

    // Generic
    RELATED_TO,            // Fallback when type is unclear
    DERIVED_FROM,          // Node created from another node
    SUPERSEDES,            // New version of a node
}
```

## Governance State

Every node and edge carries a governance state that tracks how it entered the system and how much the user trusts it.

```kotlin
enum class GovernanceState {
    CONFIRMED,  // User explicitly created or verified this
    INFERRED,   // System created based on AI analysis or pattern detection
    SEEDED,     // Imported from external source (HumanOS sync, contact import)
    DRAFT,      // User started creating but did not finalize
}
```

### Governance Rules

1. **CONFIRMED** nodes/edges are never auto-modified or auto-deleted.
2. **INFERRED** nodes/edges are shown with a visual indicator and can be promoted to CONFIRMED or dismissed.
3. **SEEDED** nodes are read-only mirrors of external data. Edits create a local CONFIRMED copy, and the SEEDED version is hidden.
4. **DRAFT** nodes auto-delete after 30 days if not promoted.
5. The user can always override any governance state.

## Privacy Integration

```kotlin
enum class PrivacyLevel {
    PUBLIC,   // Syncs normally, visible in shared views
    PRIVATE,  // Syncs but excluded from shared/team views
    VAULT,    // Never leaves device, biometric required, encrypted with device-bound key
}
```

- VAULT nodes are encrypted at rest using Android Keystore-derived keys.
- VAULT nodes are excluded from all sync operations.
- VAULT nodes require biometric authentication before display.
- Edges connecting to a VAULT node inherit VAULT privacy.

## Weight Decay

Edge weights decay over time to surface currently-relevant context and let stale connections fade.

```
weight_effective = weight_base * decay_factor^(days_since_reinforcement)
```

- `decay_factor` = 0.98 (configurable)
- Reinforcement events: user views node, creates related capture, interacts with linked person
- Minimum weight before auto-archive: 0.05
- CONFIRMED edges never auto-archive regardless of weight

## Room Schema (Phase 2)

```sql
CREATE TABLE context_nodes (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    label TEXT NOT NULL,
    summary TEXT,
    metadata TEXT NOT NULL DEFAULT '{}',   -- JSON
    governance_state TEXT NOT NULL DEFAULT 'DRAFT',
    privacy_level TEXT NOT NULL DEFAULT 'PRIVATE',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER,
    source_id TEXT,
    sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
);

CREATE TABLE context_edges (
    id TEXT PRIMARY KEY,
    source_node_id TEXT NOT NULL REFERENCES context_nodes(id),
    target_node_id TEXT NOT NULL REFERENCES context_nodes(id),
    relationship_type TEXT NOT NULL,
    weight REAL NOT NULL DEFAULT 0.5,
    governance_state TEXT NOT NULL DEFAULT 'DRAFT',
    metadata TEXT NOT NULL DEFAULT '{}',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    last_reinforced_at INTEGER,
    UNIQUE(source_node_id, target_node_id, relationship_type)
);

CREATE INDEX idx_edges_source ON context_edges(source_node_id);
CREATE INDEX idx_edges_target ON context_edges(target_node_id);
CREATE INDEX idx_nodes_type ON context_nodes(type);
CREATE INDEX idx_nodes_label ON context_nodes(label);
```

## Query Patterns (Phase 2+)

| Query | Description | Example |
|---|---|---|
| Neighbors | All nodes connected to a given node | "Who is related to Project X?" |
| Shortest path | Fewest hops between two nodes | "How do I know Dr. Garcia?" |
| Subgraph | All nodes within N hops of a seed | "Everything related to the Talca deployment" |
| Temporal | Nodes/edges created or reinforced in date range | "What happened last week?" |
| Type filter | All nodes of a given type, optionally scoped to a project | "All tasks in Project HumanOS" |
| Semantic (Phase 3) | Embedding similarity search | "What did I decide about the pricing model?" |

## Integration with HumanOS Backend

The context graph on the Android device is a local replica. Sync with the HumanOS web backend happens via the `domain-sync` module (Phase 2):

1. **Pull**: `GET /api/context/snapshot?since={timestamp}` returns changed nodes/edges.
2. **Push**: `POST /api/context/batch` sends locally-created/modified nodes/edges.
3. **Conflict**: Server is authoritative for SEEDED data. Client is authoritative for VAULT data (never sent). For CONFIRMED/INFERRED, last-write-wins with server timestamp.
4. **VAULT exclusion**: Nodes with `privacy_level = VAULT` are filtered out before any network serialization. This is enforced at the Repository layer, not the network layer, as defense in depth.
