package mega.privacy.android.feature.texteditor.components.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarkdownStyleResolverTest {

    private val cache = MarkdownEditorParseCache()

    private fun spansOf(text: String): List<MarkdownSpan> = cache.parse(text).spans

    @Test
    fun `test that resolve emits dimmed prefix and heading content for an ATX heading`() {
        val spans = spansOf("# Title")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 2, MarkdownSpanKind.Delimiter),
            MarkdownSpan(2, 7, MarkdownSpanKind.Heading(1)),
        ).inOrder()
    }

    @Test
    fun `test that resolve marks the closing hash sequence as delimiter when present`() {
        val spans = spansOf("## Title ##")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 3, MarkdownSpanKind.Delimiter),
            MarkdownSpan(3, 8, MarkdownSpanKind.Heading(2)),
            MarkdownSpan(8, 11, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve emits heading content and underline delimiter for a setext heading`() {
        val spans = spansOf("Title\n=====")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 5, MarkdownSpanKind.Heading(1)),
            MarkdownSpan(6, 11, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve splits strong emphasis into delimiters and bold content`() {
        val spans = spansOf("a **b** c")

        assertThat(spans).containsExactly(
            MarkdownSpan(2, 4, MarkdownSpanKind.Delimiter),
            MarkdownSpan(4, 5, MarkdownSpanKind.Bold),
            MarkdownSpan(5, 7, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve splits emphasis into delimiters and italic content`() {
        val spans = spansOf("*i*")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 1, MarkdownSpanKind.Delimiter),
            MarkdownSpan(1, 2, MarkdownSpanKind.Italic),
            MarkdownSpan(2, 3, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve supports GFM strikethrough`() {
        val spans = spansOf("~~s~~")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 2, MarkdownSpanKind.Delimiter),
            MarkdownSpan(2, 3, MarkdownSpanKind.Strikethrough),
            MarkdownSpan(3, 5, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve emits nested spans when bold contains italic`() {
        val spans = spansOf("**a *b* c**")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 2, MarkdownSpanKind.Delimiter),
            MarkdownSpan(2, 9, MarkdownSpanKind.Bold),
            MarkdownSpan(4, 5, MarkdownSpanKind.Delimiter),
            MarkdownSpan(5, 6, MarkdownSpanKind.Italic),
            MarkdownSpan(6, 7, MarkdownSpanKind.Delimiter),
            MarkdownSpan(9, 11, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve splits inline code into backtick delimiters and code content`() {
        val spans = spansOf("`code`")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 1, MarkdownSpanKind.Delimiter),
            MarkdownSpan(1, 5, MarkdownSpanKind.InlineCode),
            MarkdownSpan(5, 6, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve emits link text content and syntax delimiters`() {
        val spans = spansOf("[t](u)")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 1, MarkdownSpanKind.Delimiter),
            MarkdownSpan(1, 2, MarkdownSpanKind.Link("u")),
            MarkdownSpan(2, 6, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve marks fence lines as delimiters and inner lines as code`() {
        val spans = spansOf("```kotlin\nval x = 1\n```")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 9, MarkdownSpanKind.Delimiter),
            MarkdownSpan(10, 19, MarkdownSpanKind.CodeBlock),
            MarkdownSpan(20, 23, MarkdownSpanKind.Delimiter),
        ).inOrder()
    }

    @Test
    fun `test that resolve keeps all fence content lines as code when the closing fence is missing`() {
        val spans = spansOf("```\ncode")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 3, MarkdownSpanKind.Delimiter),
            MarkdownSpan(4, 8, MarkdownSpanKind.CodeBlock),
        ).inOrder()
    }

    @Test
    fun `test that resolve emits a list marker for a bullet item`() {
        val spans = spansOf("- item")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 2, MarkdownSpanKind.ListMarker),
        )
    }

    @Test
    fun `test that resolve emits a list marker covering digits and separator for an ordered item`() {
        val spans = spansOf("12. item")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 4, MarkdownSpanKind.ListMarker),
        )
    }

    @Test
    fun `test that resolve emits one quote marker per quoted line`() {
        val spans = spansOf("> one\n> two")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 2, MarkdownSpanKind.QuoteMarker),
            MarkdownSpan(6, 8, MarkdownSpanKind.QuoteMarker),
        ).inOrder()
    }

    @Test
    fun `test that resolve emits a thematic break span`() {
        val spans = spansOf("---")

        assertThat(spans).containsExactly(
            MarkdownSpan(0, 3, MarkdownSpanKind.ThematicBreak),
        )
    }

    @Test
    fun `test that resolve leaves plain paragraphs and tables unstyled`() {
        val spans = spansOf("plain text\n\n| a | b |\n|---|---|\n| 1 | 2 |")

        assertThat(spans).isEmpty()
    }
}
