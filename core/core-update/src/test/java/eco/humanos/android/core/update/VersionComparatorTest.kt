package eco.humanos.android.core.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [VersionComparator] — the heart of the update decision. These
 * are pure-JVM (no Android runtime) and lock in the tag-parsing conventions used
 * by the project's release pipeline (`vX.Y.Z-debug`).
 */
class VersionComparatorTest {

    // ── isNewer: the core decision ───────────────────────────────────────────────

    @Test
    fun `newer minor version is detected`() {
        assertThat(VersionComparator.isNewer("v0.2.0", "v0.1.0")).isTrue()
    }

    @Test
    fun `identical versions are not newer`() {
        assertThat(VersionComparator.isNewer("v0.1.0", "v0.1.0")).isFalse()
    }

    @Test
    fun `older version is not newer`() {
        assertThat(VersionComparator.isNewer("v0.1.0", "v0.2.0")).isFalse()
    }

    @Test
    fun `debug suffix is ignored so same version is not newer`() {
        // A -debug build of 0.1.0 must NOT prompt an update over plain 0.1.0.
        assertThat(VersionComparator.isNewer("v0.1.0-debug", "0.1.0")).isFalse()
        assertThat(VersionComparator.isNewer("0.1.0", "v0.1.0-debug")).isFalse()
    }

    @Test
    fun `newer version with debug suffix beats older plain version`() {
        assertThat(VersionComparator.isNewer("v0.2.0-debug", "0.1.0")).isTrue()
        assertThat(VersionComparator.isNewer("v0.2.0-debug", "v0.1.0-debug")).isTrue()
    }

    @Test
    fun `numeric (not lexicographic) comparison on minor`() {
        // Lexicographically "0.9" > "0.10"; numerically it must be the opposite.
        assertThat(VersionComparator.isNewer("v0.10.0", "v0.9.0")).isTrue()
        assertThat(VersionComparator.isNewer("v0.9.0", "v0.10.0")).isFalse()
    }

    @Test
    fun `patch-level differences are detected`() {
        assertThat(VersionComparator.isNewer("v0.1.2", "v0.1.1")).isTrue()
        assertThat(VersionComparator.isNewer("v0.1.1", "v0.1.2")).isFalse()
    }

    @Test
    fun `major version dominates`() {
        assertThat(VersionComparator.isNewer("v1.0.0", "v0.99.99")).isTrue()
        assertThat(VersionComparator.isNewer("v0.99.99", "v1.0.0")).isFalse()
    }

    // ── compare: sign contract ───────────────────────────────────────────────────

    @Test
    fun `compare returns sign per ordering`() {
        assertThat(VersionComparator.compare("v0.2.0", "v0.1.0")).isGreaterThan(0)
        assertThat(VersionComparator.compare("v0.1.0", "v0.2.0")).isLessThan(0)
        assertThat(VersionComparator.compare("v0.1.0", "v0.1.0")).isEqualTo(0)
    }

    @Test
    fun `compare ignores leading v on either side`() {
        assertThat(VersionComparator.compare("0.2.0", "v0.2.0")).isEqualTo(0)
        assertThat(VersionComparator.compare("V0.2.0", "v0.2.0")).isEqualTo(0)
    }

    // ── parse: normalization ─────────────────────────────────────────────────────

    @Test
    fun `parse strips leading v and splits components`() {
        assertThat(VersionComparator.parse("v1.2.3")).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `parse defaults missing components to zero`() {
        assertThat(VersionComparator.parse("v0.2")).containsExactly(0, 2, 0).inOrder()
        assertThat(VersionComparator.parse("1")).containsExactly(1, 0, 0).inOrder()
    }

    @Test
    fun `parse drops pre-release and build metadata`() {
        assertThat(VersionComparator.parse("v0.2.0-debug")).containsExactly(0, 2, 0).inOrder()
        assertThat(VersionComparator.parse("0.2.0-beta.1")).containsExactly(0, 2, 0).inOrder()
        assertThat(VersionComparator.parse("0.2.0+build.5")).containsExactly(0, 2, 0).inOrder()
    }

    @Test
    fun `parse tolerates whitespace and odd trailing characters`() {
        assertThat(VersionComparator.parse("  v0.2.0  ")).containsExactly(0, 2, 0).inOrder()
    }

    @Test
    fun `parse never throws on garbage input`() {
        // Defensive: a broken upstream tag resolves to 0.0.0, never an exception.
        assertThat(VersionComparator.parse("")).containsExactly(0, 0, 0).inOrder()
        assertThat(VersionComparator.parse("not-a-version")).containsExactly(0, 0, 0).inOrder()
        assertThat(VersionComparator.isNewer("garbage", "v0.1.0")).isFalse()
    }
}
