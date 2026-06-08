package eco.humanos.android.core.update

/**
 * Checks whether a newer build of the app has been published.
 *
 * Implementations are expected to be safe to call from any coroutine context and
 * to never throw: transient failures (offline, rate-limited, malformed response)
 * resolve to `null` so the caller can simply treat "no update" and "couldn't
 * check" identically from the UI's perspective.
 */
interface UpdateChecker {

    /**
     * Returns an [UpdateInfo] when the latest published release is strictly newer
     * than [currentVersionName]; otherwise returns `null` (already up to date, or
     * the check could not be completed).
     *
     * @param currentVersionName the installed version, e.g. obtained from
     *   `packageManager.getPackageInfo(packageName, 0).versionName`.
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo?
}
