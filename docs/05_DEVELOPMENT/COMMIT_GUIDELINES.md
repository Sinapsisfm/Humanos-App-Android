# Commit Guidelines

> humanOS Native Android -- Conventional Commits
> Last updated: 2026-06-06

## Format

```
type(scope): description

[optional body]

[optional footer(s)]
```

## Rules

1. **Subject line** (first line): max 72 characters, imperative mood ("add", not "added" or "adds").
2. **No period** at the end of the subject line.
3. **Blank line** between subject and body.
4. **Body** (optional): wrap at 72 characters. Explain WHAT and WHY, not HOW.
5. **Footer** (optional): references to tasks, decisions, breaking changes.

## Types

| Type | When to Use | Example |
|---|---|---|
| `feat` | New feature or capability | `feat(feature-capture): add photo capture with camera preview` |
| `fix` | Bug fix | `fix(core-sync): prevent duplicate sync when app resumes` |
| `docs` | Documentation only | `docs(architecture): update MODULE_MAP with Phase 2 modules` |
| `style` | Formatting, whitespace, no code change | `style(feature-tasks): apply ktlint formatting` |
| `refactor` | Code change that neither fixes a bug nor adds a feature | `refactor(data-capture): extract CaptureMapper to separate class` |
| `test` | Adding or updating tests | `test(core-model): add unit tests for TaskEntity mapping` |
| `chore` | Build process, tooling, dependencies | `chore(build): update Kotlin to 2.1.20` |
| `build` | Build system or external dependency changes | `build(gradle): add Health Connect SDK dependency` |
| `ci` | CI configuration changes | `ci(github): add instrumented test job to PR workflow` |
| `perf` | Performance improvement | `perf(data-capture): batch Room inserts for capture queue` |
| `revert` | Revert a previous commit | `revert: feat(feature-capture): add photo capture` |

## Scope

The scope is the **module name** affected by the change. Use kebab-case, matching the Gradle module name.

| Scope | Module |
|---|---|
| `core-model` | `:core:core-model` |
| `core-common` | `:core:core-common` |
| `core-security` | `:core:core-security` |
| `core-sync` | `:core:core-sync` |
| `core-ai` | `:core:core-ai` |
| `data-capture` | `:data:data-capture` |
| `data-tasks` | `:data:data-tasks` |
| `data-terrain` | `:data:data-terrain` |
| `data-health` | `:data:data-health` |
| `feature-capture` | `:feature:feature-capture` |
| `feature-tasks` | `:feature:feature-tasks` |
| `feature-agent` | `:feature:feature-agent` |
| `feature-dashboard` | `:feature:feature-dashboard` |
| `integration-humanos` | `:integration:integration-humanos` |
| `integration-quebot` | `:integration:integration-quebot` |
| `integration-healthconnect` | `:integration:integration-healthconnect` |
| `app` | `:app` (main application module) |
| `build` | Build system, convention plugins |
| `gradle` | Gradle wrapper, settings, version catalog |

For changes spanning multiple modules, use the most significant module or omit the scope:

```
refactor: standardize error handling across data modules
```

## Footer

### Task References

```
Refs: TASK-015
```

Multiple references:

```
Refs: TASK-015, TASK-016
```

### Decision References

```
Refs: DEC-009, ADR-0002
```

### Breaking Changes

```
BREAKING CHANGE: renamed CaptureItem.type to CaptureItem.captureType
```

Breaking changes should be rare in an Android app (no public API consumers), but are useful for tracking internal contract changes between modules.

## Examples

### Simple Feature

```
feat(core-model): add CaptureItem entity with Room annotations

Refs: TASK-012
```

### Bug Fix with Explanation

```
fix(core-sync): prevent duplicate sync when app resumes from background

The SyncManager was triggering a full sync on every onResume, even when
a sync was already in progress. Added a mutex lock and in-progress check
before starting a new sync cycle.

Refs: TASK-033
```

### Refactor

```
refactor(data-capture): extract CaptureMapper to separate class

Moved mapping logic out of CaptureRepositoryImpl into a dedicated
CaptureMapper class for testability and reuse.

Refs: TASK-012
```

### Multi-module Change

```
feat: implement offline-first task sync with conflict resolution

Adds bidirectional sync for tasks between Room and HumanOS backend.
Uses last-write-wins strategy with server timestamp as authority.
Conflicts logged to TraceEvent for audit.

Refs: TASK-028, DEC-007
```

### Dependency Update

```
build(gradle): update Kotlin to 2.1.20 and Compose BOM to 2025.05

No code changes required. All tests pass.
```

### Documentation

```
docs(architecture): update MODULE_MAP with Phase 2 modules

Added data-terrain, data-health, integration-healthconnect, and
feature-settings modules with dependency graphs.

Refs: TASK-005
```

## Squash Merge Commits

When squash-merging a feature branch to `develop`, the PR title becomes the commit message. Ensure the PR title follows this format:

```
feat(feature-capture): add photo capture with camera preview (#42)
```

The `(#42)` is the PR number, added automatically by GitHub.

## What Not to Do

```
# BAD: no type
added capture feature

# BAD: past tense
feat(capture): added photo capture

# BAD: too vague
fix: fixed bug

# BAD: too long subject
feat(feature-capture): add photo capture flow with camera preview, gallery picker, permission handling, and upload queue integration

# BAD: scope is not a module name
feat(photos): add capture

# BAD: period at end
feat(core-model): add CaptureItem entity.

# BAD: TODO without task reference in body
# TODO: clean this up later
```

## Enforcement

- **commitlint** in CI validates commit message format on PR titles.
- Squash merges use PR title as the commit message, so PR titles must follow this format.
- Direct commits to `develop` or `main` are blocked by branch protection.

## References

- BRANCHING_STRATEGY.md: Branch naming and merge strategy
- [Conventional Commits specification](https://www.conventionalcommits.org/en/v1.0.0/)
