package mega.privacy.android.feature.texteditor.components.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarkdownSyntaxFormatterTest {

    private val cache = MarkdownEditorParseCache()

    private fun apply(text: String, edit: MarkdownFormatEdit): String {
        var result = text
        edit.replacements.sortedByDescending { it.start }.forEach {
            result = result.substring(0, it.start) + it.text + result.substring(it.end)
        }
        return result
    }

    // ---- toggleInline ----

    @Test
    fun `test that toggleInline wraps a selection in bold delimiters`() {
        val text = "hello world"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 0, 5, MarkdownInlineStyle.Bold,
        )

        assertThat(apply(text, edit)).isEqualTo("**hello** world")
        assertThat(edit.selectionStart).isEqualTo(2)
        assertThat(edit.selectionEnd).isEqualTo(7)
    }

    @Test
    fun `test that toggleInline unwraps bold when the cursor is inside strong emphasis`() {
        val text = "a **b** c"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 4, 4, MarkdownInlineStyle.Bold,
        )

        assertThat(apply(text, edit)).isEqualTo("a b c")
        assertThat(edit.selectionStart).isEqualTo(2)
    }

    @Test
    fun `test that toggleInline unwraps when the selection covers the bold content`() {
        val text = "a **b** c"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 4, 5, MarkdownInlineStyle.Bold,
        )

        assertThat(apply(text, edit)).isEqualTo("a b c")
        assertThat(edit.selectionStart).isEqualTo(2)
        assertThat(edit.selectionEnd).isEqualTo(3)
    }

    @Test
    fun `test that toggleInline inserts an empty delimiter pair at a collapsed cursor`() {
        val text = "abc"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 3, 3, MarkdownInlineStyle.Bold,
        )

        assertThat(apply(text, edit)).isEqualTo("abc****")
        assertThat(edit.selectionStart).isEqualTo(5)
        assertThat(edit.selectionEnd).isEqualTo(5)
    }

    @Test
    fun `test that toggleInline wraps the trimmed range when the selection has flanking whitespace`() {
        val text = "hello world"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 5, 11, MarkdownInlineStyle.Bold,
        )

        assertThat(apply(text, edit)).isEqualTo("hello **world**")
        assertThat(edit.selectionStart).isEqualTo(8)
        assertThat(edit.selectionEnd).isEqualTo(13)
    }

    @Test
    fun `test that toggleInline unwraps strikethrough`() {
        val text = "~~gone~~"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 4, 4, MarkdownInlineStyle.Strikethrough,
        )

        assertThat(apply(text, edit)).isEqualTo("gone")
    }

    @Test
    fun `test that toggleInline unwraps inline code backticks`() {
        val text = "a `c` b"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 3, 3, MarkdownInlineStyle.Code,
        )

        assertThat(apply(text, edit)).isEqualTo("a c b")
        assertThat(edit.selectionStart).isEqualTo(2)
    }

    @Test
    fun `test that toggleInline toggles italic independently of surrounding bold`() {
        val text = "**a *b* c**"
        val edit = MarkdownSyntaxFormatter.toggleInline(
            cache.parse(text), 5, 5, MarkdownInlineStyle.Italic,
        )

        assertThat(apply(text, edit)).isEqualTo("**a b c**")
    }

    // ---- cycleHeading ----

    @Test
    fun `test that cycleHeading promotes body text to H1`() {
        val text = "body"
        val edit = MarkdownSyntaxFormatter.cycleHeading(text, 2, 2)

        assertThat(apply(text, edit)).isEqualTo("# body")
        assertThat(edit.selectionStart).isEqualTo(4)
    }

    @Test
    fun `test that cycleHeading promotes H1 to H2`() {
        val text = "# Title"
        val edit = MarkdownSyntaxFormatter.cycleHeading(text, 3, 3)

        assertThat(apply(text, edit)).isEqualTo("## Title")
        assertThat(edit.selectionStart).isEqualTo(4)
    }

    @Test
    fun `test that cycleHeading removes the prefix from H3`() {
        val text = "### T"
        val edit = MarkdownSyntaxFormatter.cycleHeading(text, 5, 5)

        assertThat(apply(text, edit)).isEqualTo("T")
        assertThat(edit.selectionStart).isEqualTo(1)
    }

    @Test
    fun `test that cycleHeading applies the first line's target level to all selected lines`() {
        val text = "one\ntwo"
        val edit = MarkdownSyntaxFormatter.cycleHeading(text, 1, 5)

        assertThat(apply(text, edit)).isEqualTo("# one\n# two")
        assertThat(edit.selectionStart).isEqualTo(3)
        assertThat(edit.selectionEnd).isEqualTo(9)
    }

    // ---- toggleList ----

    @Test
    fun `test that toggleList adds bullet markers to selected lines`() {
        val text = "one\ntwo"
        val edit = MarkdownSyntaxFormatter.toggleList(text, 0, 7, ordered = false)

        assertThat(apply(text, edit)).isEqualTo("- one\n- two")
    }

    @Test
    fun `test that toggleList removes bullet markers when all lines are bulleted`() {
        val text = "- one\n- two"
        val edit = MarkdownSyntaxFormatter.toggleList(text, 0, 11, ordered = false)

        assertThat(apply(text, edit)).isEqualTo("one\ntwo")
    }

    @Test
    fun `test that toggleList numbers ordered items sequentially`() {
        val text = "a\nb\nc"
        val edit = MarkdownSyntaxFormatter.toggleList(text, 0, 5, ordered = true)

        assertThat(apply(text, edit)).isEqualTo("1. a\n2. b\n3. c")
    }

    @Test
    fun `test that toggleList converts a bullet item to an ordered item`() {
        val text = "- a"
        val edit = MarkdownSyntaxFormatter.toggleList(text, 1, 1, ordered = true)

        assertThat(apply(text, edit)).isEqualTo("1. a")
    }

    @Test
    fun `test that toggleList skips blank lines`() {
        val text = "a\n\nb"
        val edit = MarkdownSyntaxFormatter.toggleList(text, 0, 4, ordered = false)

        assertThat(apply(text, edit)).isEqualTo("- a\n\n- b")
    }

    // ---- toggleQuote ----

    @Test
    fun `test that toggleQuote prefixes selected lines`() {
        val text = "a\nb"
        val edit = MarkdownSyntaxFormatter.toggleQuote(text, 0, 3)

        assertThat(apply(text, edit)).isEqualTo("> a\n> b")
    }

    @Test
    fun `test that toggleQuote removes the prefix when all lines are quoted`() {
        val text = "> a"
        val edit = MarkdownSyntaxFormatter.toggleQuote(text, 3, 3)

        assertThat(apply(text, edit)).isEqualTo("a")
        assertThat(edit.selectionStart).isEqualTo(1)
    }

    // ---- links ----

    @Test
    fun `test that linkAt returns the covering link`() {
        val info = MarkdownSyntaxFormatter.linkAt(cache.parse("[t](u)"), 2, 2)

        assertThat(info).isEqualTo(MarkdownLinkInfo(text = "t", url = "u", start = 0, end = 6))
    }

    @Test
    fun `test that linkAt returns null in plain text`() {
        assertThat(MarkdownSyntaxFormatter.linkAt(cache.parse("plain"), 2, 2)).isNull()
    }

    @Test
    fun `test that applyLink wraps the selection`() {
        val text = "hello"
        val edit = MarkdownSyntaxFormatter.applyLink(
            selectionStart = 0,
            selectionEnd = 5,
            existing = null,
            linkText = "hello",
            url = "u",
        )

        assertThat(apply(text, edit)).isEqualTo("[hello](u)")
        assertThat(edit.selectionStart).isEqualTo(10)
    }

    @Test
    fun `test that applyLink replaces an existing link`() {
        val text = "[t](u)"
        val edit = MarkdownSyntaxFormatter.applyLink(
            selectionStart = 2,
            selectionEnd = 2,
            existing = MarkdownLinkInfo("t", "u", 0, 6),
            linkText = "x",
            url = "v",
        )

        assertThat(apply(text, edit)).isEqualTo("[x](v)")
    }

    @Test
    fun `test that applyLink unwraps an existing link when the url is blank`() {
        val text = "[t](u)"
        val edit = MarkdownSyntaxFormatter.applyLink(
            selectionStart = 2,
            selectionEnd = 2,
            existing = MarkdownLinkInfo("t", "u", 0, 6),
            linkText = "t",
            url = "",
        )

        assertThat(apply(text, edit)).isEqualTo("t")
        assertThat(edit.selectionStart).isEqualTo(1)
    }
}
