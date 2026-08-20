package mega.privacy.android.feature.texteditor.components.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarkdownSelectionFormatsTest {

    private val cache = MarkdownEditorParseCache()

    private fun formats(text: String, start: Int, end: Int = start): MarkdownSelectionFormats =
        MarkdownSelectionFormats.from(cache.parse(text), start, end)

    @Test
    fun `test that from reports bold when the cursor is inside strong emphasis`() {
        assertThat(formats("a **b** c", 4).isBold).isTrue()
    }

    @Test
    fun `test that from reports nothing when the cursor is in plain text`() {
        assertThat(formats("a **b** c", 0)).isEqualTo(MarkdownSelectionFormats.Empty)
    }

    @Test
    fun `test that from does not report bold when the selection extends outside the emphasis`() {
        assertThat(formats("a **b** c", 1, 6).isBold).isFalse()
    }

    @Test
    fun `test that from reports bold when the selection covers exactly the emphasis`() {
        assertThat(formats("a **b** c", 2, 7).isBold).isTrue()
    }

    @Test
    fun `test that from reports both bold and italic inside nested emphasis`() {
        val result = formats("**a *b* c**", 6)
        assertThat(result.isBold).isTrue()
        assertThat(result.isItalic).isTrue()
    }

    @Test
    fun `test that from reports the heading level inside a heading`() {
        assertThat(formats("## Head", 4).headingLevel).isEqualTo(2)
    }

    @Test
    fun `test that from reports a bullet list inside a bullet item`() {
        val result = formats("- item", 3)
        assertThat(result.isBulletList).isTrue()
        assertThat(result.isOrderedList).isFalse()
    }

    @Test
    fun `test that from reports an ordered list inside a numbered item`() {
        val result = formats("1. item", 3)
        assertThat(result.isOrderedList).isTrue()
        assertThat(result.isBulletList).isFalse()
    }

    @Test
    fun `test that from reports quote inside a block quote`() {
        assertThat(formats("> quoted", 4).isQuote).isTrue()
    }

    @Test
    fun `test that from reports inline code inside a code span`() {
        assertThat(formats("a `c` b", 3).isInlineCode).isTrue()
    }

    @Test
    fun `test that from reports link inside a link`() {
        assertThat(formats("[text](url)", 2).isLink).isTrue()
    }

    @Test
    fun `test that from reports strikethrough inside a strikethrough span`() {
        assertThat(formats("~~gone~~", 3).isStrikethrough).isTrue()
    }
}
