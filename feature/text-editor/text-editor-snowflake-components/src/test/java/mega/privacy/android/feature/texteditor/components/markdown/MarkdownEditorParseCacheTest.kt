package mega.privacy.android.feature.texteditor.components.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarkdownEditorParseCacheTest {

    private val underTest = MarkdownEditorParseCache()

    @Test
    fun `test that parse returns the memoized result when the text is unchanged`() {
        val first = underTest.parse("# Title")
        val second = underTest.parse(StringBuilder("# Title"))

        assertThat(second).isSameInstanceAs(first)
    }

    @Test
    fun `test that parse re-parses when the text changes`() {
        val first = underTest.parse("# Title")
        val second = underTest.parse("# Title!")

        assertThat(second).isNotSameInstanceAs(first)
        assertThat(second.text).isEqualTo("# Title!")
    }

    @Test
    fun `test that parse exposes one range per top-level block`() {
        val result = underTest.parse("# A\n\npara")

        assertThat(result.topLevelBlocks).containsExactly(
            MarkdownBlockRange(0, 3),
            MarkdownBlockRange(5, 9),
        ).inOrder()
    }

    @Test
    fun `test that parse returns no spans or blocks for empty text`() {
        val result = underTest.parse("")

        assertThat(result.spans).isEmpty()
        assertThat(result.topLevelBlocks).isEmpty()
    }
}
