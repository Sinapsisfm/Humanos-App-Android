package eco.humanos.android.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

/**
 * [UpdateChecker] backed by the public GitHub Releases API.
 *
 * Hits `GET /repos/{owner}/{repo}/releases/latest`, which requires no auth for
 * public repos, then compares the release `tag_name` against the installed
 * version via [VersionComparator]. The download URL is the first release asset
 * whose name ends in `.apk`; if the release has no APK asset, we fall back to the
 * release page (`html_url`) so the user can still reach the download manually.
 *
 * All failures are swallowed and surfaced as `null` — a failed update check must
 * never break the screen that triggered it.
 */
class GitHubUpdateChecker @Inject constructor(
    @Named("update") private val client: OkHttpClient,
) : UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(LATEST_RELEASE_URL)
                    // GitHub recommends an explicit API version + a UA on every request.
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "humanOS-Android-UpdateChecker")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val release = json.decodeFromString<GitHubRelease>(body)
                    toUpdateInfo(release, currentVersionName)
                }
            }.getOrNull()
        }

    /**
     * Maps a parsed [GitHubRelease] to an [UpdateInfo], or `null` when the release
     * is not strictly newer than [currentVersionName] (or carries no usable tag).
     */
    private fun toUpdateInfo(release: GitHubRelease, currentVersionName: String): UpdateInfo? {
        val tag = release.tagName?.takeIf { it.isNotBlank() } ?: return null
        if (!VersionComparator.isNewer(tag, currentVersionName)) return null

        val apkUrl = release.assets
            .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?.browserDownloadUrl
            ?.takeIf { it.isNotBlank() }

        val releaseUrl = release.htmlUrl?.takeIf { it.isNotBlank() }
        // Prefer the direct APK; otherwise send the user to the release page.
        val downloadUrl = apkUrl ?: releaseUrl ?: return null

        // Normalize the tag for display (strip leading "v" / pre-release suffix).
        val (major, minor, patch) = VersionComparator.parse(tag)

        return UpdateInfo(
            versionName = "$major.$minor.$patch",
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl ?: downloadUrl,
            releaseNotes = release.body?.takeIf { it.isNotBlank() },
        )
    }

    companion object {
        /** Owner/repo are fixed: the public humanOS Android distribution repo. */
        const val REPO_OWNER = "Sinapsisfm"
        const val REPO_NAME = "Humanos-App-Android"
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }

    // ── GitHub API DTOs (only the fields we use) ─────────────────────────────────

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("body") val body: String? = null,
        @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        @SerialName("name") val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}
