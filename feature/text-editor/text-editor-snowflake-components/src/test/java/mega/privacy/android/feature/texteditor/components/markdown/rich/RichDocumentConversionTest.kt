package mega.privacy.android.feature.texteditor.components.markdown.rich

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RichDocumentConversionTest {

    private val toModel = MarkdownToRichDocumentConverter()
    private val toMarkdown = RichDocumentToMarkdownConverter()

    private fun roundTrip(markdown: String): String =
        toMarkdown.convert(toModel.convert(markdown))

    /** Model equality after a second pass — the core losslessness invariant. */
    private fun assertStable(markdown: String) {
        val first = toModel.convert(markdown)
        val serialized = toMarkdown.convert(first)
        val second = toModel.convert(serialized)
        assertThat(second).isEqualTo(first)
    }

    // ---- Markdown -> model ----

    @Test
    fun `test that convert maps inline formatting to spans over plain text`() {
        val document = toModel.convert("**b** *i* ~~s~~ `c` [t](u)")

        val paragraph = document.blocks.single() as RichBlock.Paragraph
        assertThat(paragraph.text.text).isEqualTo("b i s c t")
        assertThat(paragraph.text.spans).containsExactly(
            RichSpan(0, 1, RichSpanStyle.Bold),
            RichSpan(2, 3, RichSpanStyle.Italic),
            RichSpan(4, 5, RichSpanStyle.Strikethrough),
            RichSpan(6, 7, RichSpanStyle.Code),
            RichSpan(8, 9, RichSpanStyle.Link("u")),
        )
    }

    @Test
    fun `test that convert unescapes markdown specials into plain text`() {
        val document = toModel.convert("""2\*3 and \[brackets\]""")

        val paragraph = document.blocks.single() as RichBlock.Paragraph
        assertThat(paragraph.text.text).isEqualTo("2*3 and [brackets]")
        assertThat(paragraph.text.spans).isEmpty()
    }

    @Test
    fun `test that convert flattens nested lists into indented items`() {
        val document = toModel.convert("- a\n    - b\n- c")

        assertThat(document.blocks).containsExactly(
            RichBlock.ListItem(ordered = false, indent = 0, checked = null, RichText("a")),
            RichBlock.ListItem(ordered = false, indent = 1, checked = null, RichText("b")),
            RichBlock.ListItem(ordered = false, indent = 0, checked = null, RichText("c")),
        ).inOrder()
    }

    @Test
    fun `test that convert maps task boxes to checked state`() {
        val document = toModel.convert("- [ ] todo\n- [x] done")

        assertThat(document.blocks).containsExactly(
            RichBlock.ListItem(ordered = false, indent = 0, checked = false, RichText("todo")),
            RichBlock.ListItem(ordered = false, indent = 0, checked = true, RichText("done")),
        ).inOrder()
    }

    @Test
    fun `test that convert maps quotes with depth`() {
        val document = toModel.convert("> outer\n\n> > inner")

        assertThat(document.blocks).containsExactly(
            RichBlock.Quote(1, RichText("outer")),
            RichBlock.Quote(2, RichText("inner")),
        ).inOrder()
    }

    @Test
    fun `test that convert keeps tables as verbatim raw source`() {
        val table = "| a | b |\n|---|---|\n| 1 | 2 |"
        val document = toModel.convert("before\n\n$table\n\nafter")

        assertThat(document.blocks).containsExactly(
            RichBlock.Paragraph(RichText("before")),
            RichBlock.RawSource(table),
            RichBlock.Paragraph(RichText("after")),
        ).inOrder()
    }

    @Test
    fun `test that convert keeps paragraphs with images as raw source`() {
        val document = toModel.convert("![alt](image.png) caption")

        assertThat(document.blocks.single())
            .isEqualTo(RichBlock.RawSource("![alt](image.png) caption"))
    }

    @Test
    fun `test that convert maps code blocks with language`() {
        val document = toModel.convert("```kotlin\nval x = 1\n```")

        assertThat(document.blocks.single())
            .isEqualTo(RichBlock.CodeBlock("kotlin", "val x = 1"))
    }

    @Test
    fun `test that convert returns an empty document for blank input`() {
        assertThat(toModel.convert("")).isEqualTo(RichDocument.Empty)
        assertThat(toModel.convert("   \n  ")).isEqualTo(RichDocument.Empty)
    }

    // ---- Model -> Markdown ----

    @Test
    fun `test that convert serializes inline spans back to markdown`() {
        assertThat(roundTrip("**b** *i* ~~s~~ `c` [t](u)"))
            .isEqualTo("**b** *i* ~~s~~ `c` [t](u)")
    }

    @Test
    fun `test that convert serializes nested and equal-range spans`() {
        assertThat(roundTrip("**a *b* c**")).isEqualTo("**a *b* c**")
        assertStable("***x***")
    }

    @Test
    fun `test that convert escapes markdown specials in plain text`() {
        val document = RichDocument(
            listOf(RichBlock.Paragraph(RichText("2*3 [not a link] `tick`"))),
        )

        val markdown = toMarkdown.convert(document)

        assertThat(markdown).isEqualTo("""2\*3 \[not a link\] \`tick\`""")
        val reparsed = toModel.convert(markdown).blocks.single() as RichBlock.Paragraph
        assertThat(reparsed.text.text).isEqualTo("2*3 [not a link] `tick`")
        assertThat(reparsed.text.spans).isEmpty()
    }

    @Test
    fun `test that convert escapes block markers at line starts`() {
        val document = RichDocument(
            listOf(RichBlock.Paragraph(RichText("# not a heading"))),
        )

        val markdown = toMarkdown.convert(document)

        val reparsed = toModel.convert(markdown).blocks.single() as RichBlock.Paragraph
        assertThat(reparsed.text.text).isEqualTo("# not a heading")
    }

    @Test
    fun `test that convert numbers consecutive ordered items per indent level`() {
        val document = RichDocument(
            listOf(
                RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("a")),
                RichBlock.ListItem(ordered = true, indent = 1, checked = null, RichText("b")),
                RichBlock.ListItem(ordered = true, indent = 1, checked = null, RichText("c")),
                RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("d")),
            ),
        )

        assertThat(toMarkdown.convert(document))
            .isEqualTo("1. a\n    1. b\n    2. c\n2. d")
    }

    @Test
    fun `test that convert emits code fences longer than any backtick run in the code`() {
        val document = RichDocument(
            listOf(RichBlock.CodeBlock(null, "a ``` b")),
        )

        assertThat(toMarkdown.convert(document)).isEqualTo("````\na ``` b\n````")
    }

    @Test
    fun `test that convert escapes angle brackets so plain text is not reparsed as html`() {
        assertStable("use the <tag> placeholder")
        val reparsed = toModel.convert(
            toMarkdown.convert(
                RichDocument(listOf(RichBlock.Paragraph(RichText("a <b> c")))),
            ),
        ).blocks.single() as RichBlock.Paragraph
        assertThat(reparsed.text.text).isEqualTo("a <b> c")
    }

    @Test
    fun `test that convert wraps link urls containing spaces or parens`() {
        val document = RichDocument(
            listOf(
                RichBlock.Paragraph(
                    RichText("t", listOf(RichSpan(0, 1, RichSpanStyle.Link("u r(l)")))),
                ),
            ),
        )

        val markdown = toMarkdown.convert(document)

        assertThat(markdown).isEqualTo("[t](<u r(l)>)")
        val reparsed = toModel.convert(markdown).blocks.single() as RichBlock.Paragraph
        assertThat(reparsed.text.spans.single().style).isEqualTo(RichSpanStyle.Link("u r(l)"))
    }

    @Test
    fun `test that consecutive same-depth quotes stay separate blocks across a round trip`() {
        val document = RichDocument(
            listOf(
                RichBlock.Quote(1, RichText("first")),
                RichBlock.Quote(1, RichText("second")),
            ),
        )

        val reparsed = toModel.convert(toMarkdown.convert(document))

        assertThat(reparsed).isEqualTo(document)
    }

    // ---- Round-trip stability ----

    @Test
    fun `test that conversion is stable for a representative document`() {
        assertStable(
            """
            # Trip notes

            Planning the **spring** trip with *flexible* dates and a [site](https://mega.io).

            - Book flights
            - [x] Reserve hotel
                - [ ] Compare *prices*

            1. First
            2. Second

            > Remember ~~nothing~~ everything.

            ```kotlin
            val budget = 1200
            ```

            ---

            | City | Nights |
            |------|--------|
            | Rome | 3 |
            """.trimIndent(),
        )
    }

    @Test
    fun `test that raw source blocks survive round trips byte-identical`() {
        val table = "| a | b |\n|---|---|\n| **1** | `2` |"
        val serialized = roundTrip("intro\n\n$table")

        assertThat(serialized).contains(table)
        assertStable("intro\n\n$table")
    }
}
