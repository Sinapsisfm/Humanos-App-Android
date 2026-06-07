# User Value Priorities

> humanOS Native Android -- Ranked Value Propositions
> Last updated: 2026-06-06

## Methodology

These priorities were derived from analysis of the humanOS product vision, existing HumanOS web feature usage patterns, and the unique capabilities that a native Android app adds over the web experience. The ranking reflects what delivers the most user value per unit of implementation effort, weighted toward capabilities that only a phone can provide.

## Priority Ranking

### 1. Context Engine

**Why #1**: Everything else depends on this. Without a context model, captures are loose notes, tasks are isolated items, and the agent has no memory. The context graph is the foundation that transforms humanOS from a collection of features into a system that understands the user's world.

| Attribute | Detail |
|---|---|
| Phase | Models in Phase 1, storage + sync in Phase 2, AI inference in Phase 3 |
| Android advantage | Phone collects context signals (location, time, proximity) that desktop cannot |
| Dependency | None -- this is the foundation |
| Risk | Graph complexity may overwhelm the UI; must balance auto-inference with user control |

**Value delivered**: The user stops being the integration layer between their tools. Context connects people to projects to tasks to decisions automatically.

---

### 2. Universal Capture

**Why #2**: The phone's primary advantage over desktop is that it is always in the user's pocket. If the user can capture a thought, observation, or piece of information in under 3 seconds, the system never loses data. Everything else (context linking, task creation, follow-up) can happen asynchronously.

| Attribute | Detail |
|---|---|
| Phase | Text in Phase 1, voice + photo + share-sheet in Phase 2 |
| Android advantage | Camera, microphone, share sheet, NFC, always-on availability |
| Dependency | Context Engine (for auto-tagging captures) |
| Risk | Low -- well-understood UX patterns; main challenge is speed optimization |

**Value delivered**: Nothing the user observes, thinks, or encounters is lost. The system becomes a reliable external memory.

---

### 3. Proactive Daily Agent

**Why #3**: This is what makes humanOS an agent rather than a tool. The daily briefing and review cycle creates a habit loop that increases engagement and ensures the user stays on top of their context graph without manual effort.

| Attribute | Detail |
|---|---|
| Phase | Phase 2 (depends on sync + context engine + tasks) |
| Android advantage | Push notifications for morning briefing, background processing for preparing review |
| Dependency | Context Engine, Tasks, Sync, QueBot (for AI summary generation) |
| Risk | Medium -- quality of AI-generated summaries determines perceived value |

**Value delivered**: The user starts each day knowing what matters and ends each day knowing what was accomplished. The agent maintains continuity across days.

---

### 4. Tasks and Operative Memory

**Why #4**: Tasks are the action output of the context engine. "Operative memory" means the system remembers not just what needs to be done, but why, who delegated it, what decision led to it, and what context it belongs to. This is tasks-with-context, not a standalone todo list.

| Attribute | Detail |
|---|---|
| Phase | Phase 2 (CRUD + sync + context linking) |
| Android advantage | Quick task creation from capture, location-aware reminders, notification-based nudges |
| Dependency | Context Engine, Sync, HumanOS API |
| Risk | Low -- well-understood patterns; differentiation is in context linking, not task mechanics |

**Value delivered**: The user's commitments are tracked with full context. "Why did I agree to do this?" is always answerable.

---

### 5. Health, Energy, and Routine Awareness

**Why #5**: This is a high-value Android-exclusive capability. Health Connect, wearable data, and sensor readings are not available on desktop. An energy score that adjusts the agent's behavior (fewer tasks on low-energy days) is a genuine differentiator.

| Attribute | Detail |
|---|---|
| Phase | Phase 3 (Health Connect, sensors, energy scoring) |
| Android advantage | Exclusive -- desktop has zero access to health data |
| Dependency | Context Engine (health nodes), data-health module, core-bluetooth (wearables) |
| Risk | High -- Health Connect API is complex, health data privacy is critical, user trust is fragile |

**Value delivered**: The agent understands the user's physical state and adapts accordingly. The system becomes body-aware, not just task-aware.

---

### 6. Field Assistant

**Why #6**: High value for the primary persona (overloaded professional in clinical/consulting/engineering roles) but narrower audience than the top 5. Location tracking, geofence triggers, NFC check-in, and offline operation transform the phone into a work companion for mobile professionals.

| Attribute | Detail |
|---|---|
| Phase | Phase 2 (foreground location), Phase 3 (background location, geofence, NFC) |
| Android advantage | Exclusive -- GPS, NFC, BLE, offline mode are native mobile capabilities |
| Dependency | Context Engine (location nodes), Capture (geotagged), core-permissions |
| Risk | Medium -- battery drain from location services; background location requires strong justification for Play Store |

**Value delivered**: The user's field work is automatically logged. Context switches (arriving at clinic, leaving office) trigger relevant information surfacing.

---

### 7. Private Vault and Local AI

**Why #7**: Critical for trust but not a daily-use feature for most users. The vault provides peace of mind for sensitive data. Local AI (on-device inference) enables privacy-preserving intelligence without server round-trips. Both are important for the product's positioning but lower in daily-use frequency.

| Attribute | Detail |
|---|---|
| Phase | Vault encryption in Phase 1 (models), vault UI in Phase 3, local AI in Phase 3 |
| Android advantage | Hardware Keystore, TEE-backed encryption, on-device ML inference |
| Dependency | core-security (Keystore, biometric), domain-ai (TFLite), data-ai (embeddings) |
| Risk | High -- Keystore edge cases across OEMs, TFLite model size vs. quality tradeoff |

**Value delivered**: The user trusts the system with their most sensitive information. Local AI enables features in environments where cloud connectivity is restricted or inappropriate.

## Priority vs. Phase Matrix

```
Priority    Phase 1         Phase 2         Phase 3
--------    -------         -------         -------
1. Context  [models]        [storage+sync]  [AI inference]
2. Capture  [text only]     [voice+photo]   [auto-context]
3. Agent    [---]           [daily review]  [proactive]
4. Tasks    [---]           [CRUD+sync]     [smart suggest]
5. Health   [---]           [---]           [Health Connect]
6. Field    [---]           [foreground]    [background+NFC]
7. Vault    [models]        [---]           [vault UI+AI]
```

## Investment Allocation Guidance

| Priority Tier | % of Engineering Effort | Rationale |
|---|---|---|
| Tier 1 (Context + Capture) | 40% | Foundation -- everything depends on these |
| Tier 2 (Agent + Tasks) | 30% | Daily engagement -- drives retention |
| Tier 3 (Health + Field) | 20% | Differentiation -- Android-exclusive value |
| Tier 4 (Vault + Local AI) | 10% | Trust + future-proofing -- lower frequency, high importance |
