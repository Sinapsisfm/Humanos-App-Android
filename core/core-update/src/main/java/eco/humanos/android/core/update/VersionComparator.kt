package eco.humanos.android.core.update

/**
 * Compares semantic version strings of the form `major.minor.patch`, tolerating
 * the conventions used by this project's release tags.
 *
 * Accepted inputs (all normalize to the same `[major, minor, patch]` triple):
 *  - `"v0.2.0"`, `"0.2.0"`            → leading `v`/`V` is stripped
 *  - `"v0.2.0-debug"`, `"0.2.0-beta"` → any `-suffix` / `+build` metadata is dropped
 *  - `"v0.2"`, `"0.2"`                → missing components default to 0 → `0.2.0`
 *  - `"v1"`                           → `1.0.0`
 *
 * Comparison is numeric per component (so `0.10.0 > 0.9.0`). Pre-release and
 * build metadata are intentionally ignored: a `-debug` build of `0.2.0` is
 * considered the same version as a plain `0.2.0`. This is deliberate for a
 * debug-distribution channel where the tag carries the version of record.
 *
 * Unparseable components are treated as 0 rather than throwing, so a malformed
 * upstream tag can never crash the update check — it simply won't register as
 * "newer".
 */
object VersionComparator {

    /**
     * @return a negative number if [a] < [b], zero if equal, a positive number if
     *   [a] > [b], ignoring any pre-release/build suffix.
     */
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        for (i in 0 until 3) {
            val diff = pa[i] - pb[i]
            if (diff != 0) return diff
        }
        return 0
    }

    /** True when [candidate] is a strictly newer version than [current]. */
    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    /**
     * Normalizes a tag/version string to a `[major, minor, patch]` triple.
     * Always returns a list of exactly three non-negative ints.
     */
    fun parse(version: String): List<Int> {
        // 1. Trim whitespace and any leading "v"/"V".
        var core = version.trim()
        if (core.startsWith("v", ignoreCase = true)) {
            core = core.substring(1)
        }
        // 2. Drop SemVer pre-release ("-debug", "-beta.1") and build ("+sha") metadata.
        core = core.substringBefore('-').substringBefore('+').trim()

        // 3. Split into up to three numeric components, defaulting missing ones to 0.
        val parts = core.split('.')
        val major = parts.getOrNull(0).toVersionInt()
        val minor = parts.getOrNull(1).toVersionInt()
        val patch = parts.getOrNull(2).toVersionInt()
        return listOf(major, minor, patch)
    }

    /**
     * Parses a single version component to a non-negative Int. Strips any trailing
     * non-digit characters (defensive against odd tags) and falls back to 0 when
     * nothing numeric remains.
     */
    private fun String?.toVersionInt(): Int {
        if (this == null) return 0
        val digits = trim().takeWhile { it.isDigit() }
        return digits.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }
}
