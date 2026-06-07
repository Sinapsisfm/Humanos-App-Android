# Branching Strategy

> humanOS Native Android -- Git Flow (Adapted)
> Last updated: 2026-06-06

## Branch Types

| Branch | Purpose | Lifetime | Created From | Merges Into |
|---|---|---|---|---|
| `main` | Production-ready code. Every commit is releasable. | Permanent | -- | -- |
| `develop` | Integration branch. All features merge here first. | Permanent | `main` | `main` (via release branch) |
| `feature/*` | New features and enhancements | Temporary | `develop` | `develop` |
| `release/*` | Release preparation (version bump, changelog, final fixes) | Temporary | `develop` | `main` + `develop` |
| `hotfix/*` | Urgent production fixes | Temporary | `main` | `main` + `develop` |

## Branch Flow

```
main ─────────────────────────────●───────────●──────────────
                                  ↑           ↑
                           release/1.0   hotfix/1.0.1
                                  ↑
develop ──●────●────●────●────────●────●────●────●───────────
           ↑    ↑    ↑    ↑              ↑    ↑
         feat  feat  feat  feat        feat  feat
         /A    /B    /C    /D          /E    /F
```

## Naming Conventions

### Feature Branches

```
feature/TASK-{number}-{short-description}
```

Examples:
- `feature/TASK-001-setup-project-skeleton`
- `feature/TASK-015-capture-photo-flow`
- `feature/TASK-042-health-connect-import`

Rules:
- Always reference a task number from TASKS.md.
- Short description in kebab-case, max 5 words.
- No author names in branch names.

### Release Branches

```
release/{major}.{minor}.{patch}
```

Examples:
- `release/1.0.0`
- `release/1.1.0`

### Hotfix Branches

```
hotfix/{major}.{minor}.{patch}
```

Examples:
- `hotfix/1.0.1`
- `hotfix/1.1.1`

## Merge Strategy

| Source | Target | Merge Type | Rationale |
|---|---|---|---|
| `feature/*` | `develop` | **Squash merge** | Clean history, one commit per feature |
| `develop` | `release/*` | Branch creation (no merge) | Release branch is cut from develop |
| `release/*` | `main` | **Merge commit** | Preserves release boundary in history |
| `release/*` | `develop` | **Merge commit** | Back-merge release fixes |
| `hotfix/*` | `main` | **Merge commit** | Preserves hotfix boundary |
| `hotfix/*` | `develop` | **Merge commit** | Back-merge hotfix |

### Why Squash Merge for Features?

- Feature branches often have messy WIP commits ("fix typo", "debugging", "actually fix it").
- Squash merge produces one clean commit with a conventional commit message.
- The full branch history is still available in the closed PR for archaeology.

### Why Merge Commit for Releases and Hotfixes?

- Merge commits create clear boundary markers in `main` history.
- `git log --first-parent main` shows a clean timeline of releases.
- Easier to identify which release introduced a change.

## Pull Request Requirements

All merges to `develop` and `main` go through Pull Requests. No direct pushes.

### PR Checklist

- [ ] Branch is up to date with target (rebase or merge target into branch)
- [ ] All CI checks pass (lint, tests, build)
- [ ] PR title follows conventional commit format: `type(scope): description`
- [ ] PR description includes: what changed, why, how to test
- [ ] No unresolved TODO without task reference
- [ ] No `println` or `Log.d` debug statements (use structured logging)

### Review Requirements

| Target Branch | Minimum Reviews | Reviewer Requirement |
|---|---|---|
| `develop` | 1 | Any team member |
| `main` (via release) | 1 | Project lead or designated reviewer |
| `main` (via hotfix) | 1 | Any team member (expedited) |

## Workflow Examples

### Starting a New Feature

```bash
git checkout develop
git pull origin develop
git checkout -b feature/TASK-015-capture-photo-flow
# ... work ...
git push -u origin feature/TASK-015-capture-photo-flow
# Open PR targeting develop
```

### Preparing a Release

```bash
git checkout develop
git pull origin develop
git checkout -b release/1.0.0
# Bump version in build.gradle.kts
# Update CHANGELOG.md
# Final bug fixes only (no new features)
git push -u origin release/1.0.0
# Open PR targeting main
# After merge to main: tag vX.Y.Z
# Back-merge to develop
```

### Hotfix

```bash
git checkout main
git pull origin main
git checkout -b hotfix/1.0.1
# Fix the issue
# Bump patch version
git push -u origin hotfix/1.0.1
# Open PR targeting main
# After merge to main: tag vX.Y.Z
# Open PR targeting develop (back-merge)
```

## Branch Protection Rules

| Branch | Rules |
|---|---|
| `main` | Require PR, require CI pass, no force push, no delete |
| `develop` | Require PR, require CI pass, no force push, no delete |
| `feature/*` | No restrictions (developer's workspace) |
| `release/*` | Require PR to main, require CI pass |
| `hotfix/*` | Require PR to main, require CI pass |

## Stale Branch Cleanup

- Feature branches deleted after PR merge (GitHub auto-delete).
- Release branches deleted after back-merge to develop.
- Hotfix branches deleted after back-merge to develop.
- Monthly audit: delete any branch inactive for > 30 days (after confirming with author).

## References

- COMMIT_GUIDELINES.md: Conventional commit format for PR titles and squash commits
- RELEASE_CHECKLIST.md: Full release process
