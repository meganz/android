package mega.privacy.android.feature.texteditor.components.markdown.rich

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RichDocumentStateTest {

    private val document = RichDocument(
        listOf(
            RichBlock.Heading(1, RichText("Title")),
            RichBlock.Paragraph(RichText("body", listOf(RichSpan(0, 4, RichSpanStyle.Bold)))),
            RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("one")),
            RichBlock.ListItem(ordered = false, indent = 1, checked = false, RichText("task")),
            RichBlock.Quote(2, RichText("quoted")),
            RichBlock.CodeBlock("kotlin", "val x = 1"),
            RichBlock.ThematicBreak,
            RichBlock.RawSource("| a |\n|---|"),
        ),
    )

    @Test
    fun `test that toDocument round-trips every block kind`() {
        val state = RichDocumentState(document)

        assertThat(state.toDocument()).isEqualTo(document)
    }

    @Test
    fun `test that toDocument reflects text edits made in a block state`() {
        val state = RichDocumentState(document)
        val heading = state.blocks[0] as RichTextBlockEditState
        heading.text.textFieldState.edit { append("!") }

        val heading2 = state.toDocument().blocks[0] as RichBlock.Heading
        assertThat(heading2.text.text).isEqualTo("Title!")
    }

    @Test
    fun `test that toDocument reflects a checkbox kind change`() {
        val state = RichDocumentState(document)
        val task = state.blocks[3] as RichTextBlockEditState
        task.kind = (task.kind as RichBlockKind.Item).copy(checked = true)

        val item = state.toDocument().blocks[3] as RichBlock.ListItem
        assertThat(item.checked).isTrue()
    }

    @Test
    fun `test that focusedTextBlock returns the focused text block only`() {
        val state = RichDocumentState(document)

        assertThat(state.focusedTextBlock).isNull()
        state.focusedIndex = 1
        assertThat(state.focusedTextBlock).isSameInstanceAs(state.blocks[1])
        state.focusedIndex = 5 // code block: not a text block
        assertThat(state.focusedTextBlock).isNull()
    }
}
