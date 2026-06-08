package eco.humanos.android.core.database.converter

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for the [Converters] `List<String>` <-> JSON String TypeConverter.
 *
 * Verifies round-trip fidelity for empty, single, multi-element, and
 * special-character payloads, plus the documented null-handling contract.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `empty list round-trips`() {
        val original = emptyList<String>()

        val result = converters.toStringList(converters.fromStringList(original))

        assertThat(result).isEqualTo(original)
        assertThat(result).isEmpty()
    }

    @Test
    fun `single element list round-trips`() {
        val original = listOf("hello")

        val result = converters.toStringList(converters.fromStringList(original))

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `multiple element list round-trips in order`() {
        val original = listOf("alpha", "beta", "gamma", "delta")

        val result = converters.toStringList(converters.fromStringList(original))

        assertThat(result).isEqualTo(original)
        assertThat(result).containsExactly("alpha", "beta", "gamma", "delta").inOrder()
    }

    @Test
    fun `elements with special characters round-trip`() {
        val original = listOf(
            "a,b,c",
            "has \"double quotes\"",
            "has 'single quotes'",
            "line\nbreak",
            "tab\tseparated",
            "backslash\\here",
            "unicode: ñ á é í ó ú 中文 😀",
            "json-like: {\"key\": [1, 2, 3]}",
            "",
        )

        val result = converters.toStringList(converters.fromStringList(original))

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `comma containing elements are not split apart`() {
        val original = listOf("one,two", "three")

        val result = converters.toStringList(converters.fromStringList(original))

        assertThat(result).hasSize(2)
        assertThat(result).containsExactly("one,two", "three").inOrder()
    }

    @Test
    fun `fromStringList produces a JSON array string`() {
        val json = converters.fromStringList(listOf("x", "y"))

        assertThat(json).isEqualTo("""["x","y"]""")
    }

    @Test
    fun `fromStringList of null returns null`() {
        assertThat(converters.fromStringList(null)).isNull()
    }

    @Test
    fun `toStringList of null returns empty list`() {
        assertThat(converters.toStringList(null)).isEmpty()
    }

    @Test
    fun `null fromStringList then toStringList yields empty list`() {
        // A null column survives the documented contract: null -> null -> emptyList.
        val result = converters.toStringList(converters.fromStringList(null))

        assertThat(result).isEmpty()
    }
}
