# Auto-Update (Debug Distribution)

> humanOS Native Android -- In-app update checker + CI release publishing
> Last updated: 2026-06-08

## Overview

Felipe no longer needs to hand the APK around manually. The app checks GitHub
Releases for a newer build and prompts the user to download it; a CI workflow
builds and publishes the APK automatically whenever a version tag is pushed.

Two moving parts:

1. **In-app update checker** (`:core:core-update`) -- on the Settings screen the
   app queries the public GitHub Releases API and, if a newer version exists,
   shows a "Nueva version vX.Y.Z disponible" card with a **Descargar
   actualizacion** button. The button opens the APK download URL in the browser;
   the user taps the downloaded file to install (sideload).
2. **Release CI** (`.github/workflows/release-apk.yml`) -- on a pushed tag
   matching `v*`, GitHub Actions builds `assembleDebug` and attaches the APK to
   the auto-created GitHub Release. Release assets have no 10 MB browser-upload
   limit, so the full APK is always available.

Repo: `Sinapsisfm/Humanos-App-Android` (public -- the Releases API needs no auth).

## How the in-app checker works

- Module: `:core:core-update`
  - `UpdateChecker` (interface) + `GitHubUpdateChecker` (OkHttp impl)
  - `GET https://api.github.com/repos/Sinapsisfm/Humanos-App-Android/releases/latest`
  - `VersionComparator` parses the release `tag_name` (e.g. `v0.2.0-debug`),
    strips the leading `v` and any `-suffix`/`+build` metadata, and compares
    `major.minor.patch` numerically against the installed version.
  - Download URL = the release asset whose name ends in `.apk`; if none, it falls
    back to the release page (`html_url`).
  - Failures (offline, rate-limited, malformed) resolve to "no update" -- the
    check never throws and never blocks the UI.
- The installed version is read at runtime via
  `packageManager.getPackageInfo(packageName, 0).versionName` -- no BuildConfig
  wiring required.
- Surfaced in `feature-settings`: `SettingsViewModel.checkForUpdate()` runs on
  init; `SettingsScreen` renders the banner when an update exists, or shows "App
  actualizada" subtly in the version row otherwise.

> Note on the `-debug` suffix: a `-debug` build of `0.2.0` is treated as the same
> version as a plain `0.2.0`. The numeric `major.minor.patch` is the version of
> record, so always bump those numbers between releases or the prompt won't fire.

## One-time setup (Felipe) -- the ONLY manual step

Add a GitHub Actions secret so the released APK contains the Firebase config
(`google-services.json`), which Google Sign-In needs. `google-services.json` is
gitignored, so CI can't read it from the repo -- it comes from this secret.

1. Produce the base64 of the file.

   - **macOS / Linux / Git Bash:**
     ```bash
     base64 -w0 app/google-services.json
     ```
     (on macOS, `base64 -i app/google-services.json` -- it has no line wrapping by default)

   - **Windows (PowerShell), no newlines:**
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json")) | Set-Clipboard
     ```

   - **Windows (`certutil`)** -- produces a file with header/footer lines you must
     strip; the PowerShell one-liner above is preferred:
     ```cmd
     certutil -encode app\google-services.json gs.b64
     ```

2. In the GitHub repo: **Settings -> Secrets and variables -> Actions -> New
   repository secret**.
   - **Name:** `GOOGLE_SERVICES_JSON`
   - **Value:** the base64 string from step 1.

That's it. If the secret is absent, CI still builds and publishes a working APK,
but Google Sign-In will not function in it (the workflow logs a warning, and the
`google-services` plugin is skipped because the file is missing -- see
`app/build.gradle.kts`).

## Release flow (every new version)

1. Bump the version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2          // increment by 1
   versionName = "0.2.0"    // the number the checker compares
   ```
2. Commit the bump.
3. Tag and push:
   ```bash
   git tag v0.2.0-debug
   git push origin v0.2.0-debug
   ```
4. CI (`release-apk.yml`) builds `assembleDebug` and publishes the APK as
   `humanOS-v0.2.0-debug.apk` on a GitHub Release for that tag.
5. Users open Settings; the app detects `v0.2.0-debug` > their installed version
   and shows the download prompt.

### Tag vs. versionName

The checker compares numbers, ignoring the `-debug` suffix. So tag `v0.2.0-debug`
with `versionName = "0.2.0"` is consistent. Keep the tag's numeric part equal to
`versionName`. Bumping only the suffix (e.g. `v0.2.0-debug` -> `v0.2.0-debug2`)
will **not** trigger an update -- bump `major.minor.patch`.

## Sideload note

The downloaded APK installs via the system package installer. The user must allow
"install unknown apps" for their browser the first time. This is expected for
debug distribution outside the Play Store.

## Files

- `core/core-update/` -- `UpdateChecker`, `GitHubUpdateChecker`, `UpdateInfo`,
  `VersionComparator`, `di/UpdateModule`
- `feature/feature-settings/` -- `SettingsViewModel`, `SettingsScreen` (banner)
- `.github/workflows/release-apk.yml` -- the release pipeline
