# Release Checklist

> humanOS Native Android -- Release Process
> Last updated: 2026-06-06

## Pre-Release

Complete all items before cutting the release branch.

### Code Quality

- [ ] All unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] All instrumented tests pass (`./gradlew connectedDebugAndroidTest`)
- [ ] ktlint clean (`./gradlew ktlintCheck`) -- zero violations
- [ ] detekt clean (`./gradlew detekt`) -- zero new violations
- [ ] Android Lint clean (`./gradlew lintDebug`) -- zero errors, warnings reviewed
- [ ] No `println` or `Log.d` debug statements in production code
- [ ] No TODO without TASK reference (`grep -r "TODO" --include="*.kt" | grep -v "TASK-"` returns empty)
- [ ] No FIXME without TASK reference
- [ ] No commented-out code blocks

### Documentation

- [ ] `CURRENT_STATE.md` updated with accurate project status
- [ ] `CHANGELOG.md` updated with all changes since last release
- [ ] `MODULE_MAP.md` reflects actual module structure
- [ ] `DECISIONS_LOG.md` includes any new decisions made during this cycle
- [ ] `RISKS.md` reviewed -- no unmitigated critical risks

### Dependencies

- [ ] No known vulnerable dependencies (`./gradlew dependencyCheckAnalyze`)
- [ ] All dependencies on stable versions (no SNAPSHOT, alpha, beta in release)
- [ ] `libs.versions.toml` version catalog is clean and consistent

### Build

- [ ] Release build compiles (`./gradlew assembleRelease`)
- [ ] AAB builds successfully (`./gradlew bundleRelease`)
- [ ] ProGuard/R8 rules verified -- no crashes in minified build
- [ ] App size within budget (APK < 30MB, AAB < 25MB per ABI)

### Functional

- [ ] Manual smoke test on physical device (minimum: Pixel-class + Samsung)
- [ ] All deep links work (`adb shell am start -d "humanos://..."`)
- [ ] Notification channels appear correctly in system Settings
- [ ] Offline mode works -- app launches and shows cached data without network
- [ ] Fresh install flow works -- onboarding, permissions, first sync

## Release Process

### 1. Cut Release Branch

```bash
git checkout develop
git pull origin develop
git checkout -b release/X.Y.Z
```

### 2. Bump Version

Update in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = <increment>     // always increment, never reuse
        versionName = "X.Y.Z"
    }
}
```

Commit:

```
chore(app): bump version to X.Y.Z

Refs: TASK-XXX
```

### 3. Update CHANGELOG

Add entry to `CHANGELOG.md`:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- ...

### Changed
- ...

### Fixed
- ...

### Removed
- ...
```

Commit:

```
docs: update CHANGELOG for vX.Y.Z release
```

### 4. Final Fixes

Only bug fixes allowed on the release branch. No new features.

### 5. Merge to Main

```bash
# Open PR: release/X.Y.Z → main
# Merge type: merge commit (not squash)
```

### 6. Tag

```bash
git checkout main
git pull origin main
git tag -a vX.Y.Z -m "Release X.Y.Z"
git push origin vX.Y.Z
```

### 7. Back-Merge to Develop

```bash
git checkout develop
git pull origin develop
git merge main
git push origin develop
```

### 8. Build and Distribute

**Beta (Firebase App Distribution):**

```bash
./gradlew bundleRelease
# Upload AAB to Firebase App Distribution via CI or manually
# Notify beta testers
```

**Production (Play Console):**

```bash
./gradlew bundleRelease
# Upload AAB to Google Play Console
# Target: Internal testing → Closed beta → Open beta → Production
# Staged rollout: 5% → 20% → 50% → 100%
```

### 9. Clean Up

- [ ] Delete release branch
- [ ] Close associated milestone in GitHub
- [ ] Update CURRENT_STATE.md on develop

## Post-Release Monitoring

### First 24 Hours

- [ ] Monitor Crashlytics for new crashes (target: > 99.5% crash-free)
- [ ] Check Play Console for ANRs (target: < 0.5% ANR rate)
- [ ] Review Play Console user reviews for new issues
- [ ] Verify WorkManager tasks executing correctly (sync, briefing)
- [ ] Check FCM delivery rates in Firebase Console

### First Week

- [ ] Review crash-free rate trend (should not decline)
- [ ] Monitor Play Console vitals (startup time, battery, permissions)
- [ ] Collect beta tester feedback
- [ ] Decide on rollout percentage increase (if staged)
- [ ] Document any issues in RISKS.md

### Rollback Criteria

Halt rollout and prepare hotfix if any of:
- Crash-free rate drops below 99%
- ANR rate exceeds 1%
- Data loss reported by any user
- Authentication failures prevent sign-in
- Sync completely broken (no data reaching server)

## Version Numbering

Follows [Semantic Versioning](https://semver.org/):

| Component | When to Increment | Example |
|---|---|---|
| **Major** (X.0.0) | Breaking changes, major redesign | 1.0.0 → 2.0.0 |
| **Minor** (0.X.0) | New features, backward compatible | 1.0.0 → 1.1.0 |
| **Patch** (0.0.X) | Bug fixes, minor improvements | 1.0.0 → 1.0.1 |

`versionCode` (integer) always increments by 1, regardless of version name changes.

## Play Store Listing Updates

When updating the Play Store listing alongside a release:

- [ ] Screenshots updated (if UI changed significantly)
- [ ] Feature graphic updated (if branding changed)
- [ ] Short description accurate
- [ ] Full description reflects current features
- [ ] What's New text written for this release
- [ ] Data Safety section accurate (if permissions or data handling changed)
- [ ] Content rating questionnaire updated (if content type changed)

## Firebase App Distribution (Beta)

For pre-production testing:

```bash
# Via Gradle plugin
./gradlew appDistributionUploadDebug

# Or via Firebase CLI
firebase appdistribution:distribute app-debug.apk \
  --app <firebase-app-id> \
  --groups "internal-testers" \
  --release-notes "Beta vX.Y.Z: ..."
```

Beta testers are defined in Firebase Console under App Distribution groups.

## References

- BRANCHING_STRATEGY.md: Release branch workflow
- COMMIT_GUIDELINES.md: Version bump commit format
- CURRENT_STATE.md: Project status document
