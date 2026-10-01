package mega.privacy.android.shared.nodes.namecollision.view

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DuplicateItemsNameFormatTest {

    @Test
    fun `test that truncateMiddleKeepingExtension returns the name unchanged when it fits`() {
        val name = "Holiday photos 2026.jpeg"

        assertThat(name.truncateMiddleKeepingExtension(isFile = true)).isEqualTo(name)
        assertThat(name.truncateMiddleKeepingExtension(isFile = false)).isEqualTo(name)
    }

    @Test
    fun `test that truncateMiddleKeepingExtension keeps the extension when a long file name is shortened`() {
        val name = "Quarterly financial report for the board meeting 2026 final reviewed copy.pdf"

        val result = name.truncateMiddleKeepingExtension(isFile = true)

        assertThat(result).isEqualTo("Quarterly financia…nal reviewed copy.pdf")
        assertThat(result).endsWith(".pdf")
        assertThat(result).hasLength(40)
    }

    @Test
    fun `test that truncateMiddleKeepingExtension shortens the whole name when it is a folder`() {
        val name = "Quarterly financial reports for the board meetings of 2026.archive"

        val result = name.truncateMiddleKeepingExtension(isFile = false)

        assertThat(result).isEqualTo("Quarterly financial …ngs of 2026.archive")
        assertThat(result).hasLength(40)
    }

    @Test
    fun `test that truncateMiddleKeepingExtension shortens the whole name when the extension is longer than eight characters`() {
        val name = "a".repeat(30) + ".abcdefghijk"

        val result = name.truncateMiddleKeepingExtension(isFile = true)

        assertThat(result).isEqualTo("a".repeat(20) + "…" + "a".repeat(7) + ".abcdefghijk")
        assertThat(result).hasLength(40)
    }

    @Test
    fun `test that truncateMiddleKeepingExtension shortens the whole name when it is a dotfile`() {
        val name = "." + "x".repeat(45)

        val result = name.truncateMiddleKeepingExtension(isFile = true)

        assertThat(result).isEqualTo("." + "x".repeat(19) + "…" + "x".repeat(19))
        assertThat(result).hasLength(40)
    }

    @Test
    fun `test that truncateMiddleKeepingExtension never exceeds the maximum length`() {
        val names = listOf(
            "b".repeat(41),
            "b".repeat(41) + ".txt",
            "b".repeat(100) + ".tar.gz",
            "b".repeat(12) + ".abcdefgh",
            "b".repeat(30) + ".",
            "b".repeat(50) + ".verylongextension",
        )

        names.forEach { name ->
            assertThat(name.truncateMiddleKeepingExtension(isFile = true, maxLength = 20).length)
                .isAtMost(20)
            assertThat(name.truncateMiddleKeepingExtension(isFile = false, maxLength = 20).length)
                .isAtMost(20)
        }
    }
}
