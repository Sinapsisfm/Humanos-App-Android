# Architecture Decision Records

> humanOS Native Android -- ADR Index
> Last updated: 2026-06-06

## What is an ADR?

An Architecture Decision Record captures a significant architectural decision along with its context and consequences. ADRs are immutable once accepted -- if a decision is reversed, it is superseded by a new ADR, not edited.

## Format

Each ADR follows this structure:

```markdown
# ADR-NNNN: Title

**Status:** Proposed | Accepted | Deprecated | Superseded by ADR-NNNN

**Date:** YYYY-MM-DD

**Deciders:** [who was involved]

## Context

What is the issue that we are seeing that motivates this decision?

## Decision

What is the change that we are proposing and/or doing?

## Consequences

What becomes easier or harder as a result of this decision?
```

## Status Definitions

| Status | Meaning |
|---|---|
| **Proposed** | Under discussion, not yet agreed upon |
| **Accepted** | Agreed upon and in effect |
| **Deprecated** | No longer relevant (project evolved past it) |
| **Superseded** | Replaced by a newer ADR (link to replacement) |

## ADR Index

| ADR | Title | Status | Date |
|---|---|---|---|
| [ADR-0001](ADR-0001-project-separation.md) | Project Separation from HumanOS and QueBot | Accepted | 2026-06-06 |
| [ADR-0002](ADR-0002-modular-android-architecture.md) | Modular Android Architecture with Convention Plugins | Accepted | 2026-06-06 |
| [ADR-0003](ADR-0003-readonly-integrations.md) | Read-Only Integration Strategy | Accepted | 2026-06-06 |
| [ADR-0004](ADR-0004-traceability-first.md) | Traceability as a Product Feature | Accepted | 2026-06-06 |

## Creating a New ADR

1. Copy the template above.
2. Use the next sequential number: `ADR-NNNN`.
3. File name: `ADR-NNNN-short-description.md` (kebab-case).
4. Set status to `Proposed`.
5. Add entry to this index.
6. Discuss with team. When agreed, change status to `Accepted` and set the date.
7. Commit with: `docs(adr): add ADR-NNNN short description`.

## Relationship to DECISIONS_LOG.md

ADRs capture **architectural** decisions (technology choices, module boundaries, integration patterns). The `DECISIONS_LOG.md` in `00_PROJECT_CONTROL/` captures **all** decisions including tactical ones (naming choices, library preferences, scope cuts). Every ADR should have a corresponding entry in DECISIONS_LOG.md, but not every decision warrants a full ADR.
