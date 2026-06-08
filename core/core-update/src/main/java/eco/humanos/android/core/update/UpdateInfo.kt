package eco.humanos.android.core.update

/**
 * Describes an available app update discovered from the GitHub Releases API.
 *
 * Only produced when the latest published release is strictly newer than the
 * currently installed version (see [VersionComparator]).
 *
 * @property versionName the release tag, normalized for display (e.g. "0.2.0").
 * @property downloadUrl direct URL the user taps to download the APK. This is the
 *   release asset ending in `.apk` when present, otherwise the release page URL.
 * @property releaseUrl the human-facing GitHub release page (`html_url`).
 * @property releaseNotes the release body / changelog, or null when empty.
 */
data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val releaseNotes: String?,
)
