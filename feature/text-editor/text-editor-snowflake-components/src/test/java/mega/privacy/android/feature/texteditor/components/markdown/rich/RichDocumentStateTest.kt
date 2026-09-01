package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.ui.text.TextRange
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
    fun `test that splitBlock moves trailing text and spans into a new block`() {
        val state = stateOf(
            RichBlock.Paragraph(
                RichText("bold plain", listOf(RichSpan(0, 4, RichSpanStyle.Bold))),
            ),
        )

        state.splitBlock(0, 5)

        val first = state.blocks[0] as RichTextBlockEditState
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(first.text.textFieldState.text.toString()).isEqualTo("bold ")
        assertThat(first.text.spans).containsExactly(RichSpan(0, 4, RichSpanStyle.Bold))
        assertThat(second.kind).isEqualTo(RichBlockKind.Paragraph)
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("plain")
        assertThat(second.text.spans).isEmpty()
        assertThat(state.pendingFocus).isEqualTo(RichFocusRequest(1, TextRange.Zero))
    }

    @Test
    fun `test that splitBlock splits a span straddling the split point`() {
        val state = stateOf(
            RichBlock.Paragraph(
                RichText("aabbcc", listOf(RichSpan(1, 5, RichSpanStyle.Italic))),
            ),
        )

        state.splitBlock(0, 3)

        val first = state.blocks[0] as RichTextBlockEditState
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(first.text.spans).containsExactly(RichSpan(1, 3, RichSpanStyle.Italic))
        assertThat(second.text.spans).containsExactly(RichSpan(0, 2, RichSpanStyle.Italic))
    }

    @Test
    fun `test that splitBlock removes the replaced selection range`() {
        val state = stateOf(RichBlock.Paragraph(RichText("hello world")))

        state.splitBlock(0, 2, 8)

        val first = state.blocks[0] as RichTextBlockEditState
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(first.text.textFieldState.text.toString()).isEqualTo("he")
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("rld")
    }

    @Test
    fun `test that splitBlock at the end of a heading starts a paragraph`() {
        val state = stateOf(RichBlock.Heading(2, RichText("Title")))

        state.splitBlock(0, 5)

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Heading(2))
        assertThat((state.blocks[1] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that splitBlock inside a heading keeps the heading kind on both halves`() {
        val state = stateOf(RichBlock.Heading(1, RichText("Title")))

        state.splitBlock(0, 2)

        assertThat((state.blocks[1] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Heading(1))
    }

    @Test
    fun `test that splitBlock continues a list item and resets its task state`() {
        val state = stateOf(
            RichBlock.ListItem(ordered = true, indent = 1, checked = true, RichText("task")),
        )

        state.splitBlock(0, 4)

        assertThat((state.blocks[1] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = true, indent = 1, checked = false))
    }

    @Test
    fun `test that splitBlock on an empty list item steps out instead of splitting`() {
        val state = stateOf(
            RichBlock.ListItem(ordered = false, indent = 1, checked = null, RichText("")),
        )

        state.splitBlock(0, 0)
        assertThat(state.blocks).hasSize(1)
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = null))

        state.splitBlock(0, 0)
        assertThat(state.blocks).hasSize(1)
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that splitBlock on an empty paragraph adds a new empty paragraph`() {
        val state = stateOf(RichBlock.Paragraph(RichText("")))

        state.splitBlock(0, 0)

        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that mergeBlockBackward demotes a list item before merging`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first")),
            RichBlock.ListItem(ordered = false, indent = 0, checked = null, RichText("item")),
        )

        assertThat(state.mergeBlockBackward(1)).isTrue()
        assertThat((state.blocks[1] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that mergeBlockBackward reduces quote depth one level at a time`() {
        val state = stateOf(RichBlock.Quote(2, RichText("quoted")))
        val block = state.blocks[0] as RichTextBlockEditState

        assertThat(state.mergeBlockBackward(0)).isTrue()
        assertThat(block.kind).isEqualTo(RichBlockKind.Quote(1))

        assertThat(state.mergeBlockBackward(0)).isTrue()
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that mergeBlockBackward joins a paragraph into the previous one`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first")),
            RichBlock.Paragraph(
                RichText("second", listOf(RichSpan(0, 6, RichSpanStyle.Bold))),
            ),
        )

        assertThat(state.mergeBlockBackward(1)).isTrue()

        assertThat(state.blocks).hasSize(1)
        val merged = state.blocks[0] as RichTextBlockEditState
        assertThat(merged.text.textFieldState.text.toString()).isEqualTo("firstsecond")
        assertThat(merged.text.spans).containsExactly(RichSpan(5, 11, RichSpanStyle.Bold))
        assertThat(state.pendingFocus).isEqualTo(RichFocusRequest(0, TextRange(5)))
    }

    @Test
    fun `test that mergeBlockBackward removes a preceding thematic break`() {
        val state = stateOf(
            RichBlock.ThematicBreak,
            RichBlock.Paragraph(RichText("after")),
        )

        assertThat(state.mergeBlockBackward(1)).isTrue()

        assertThat(state.blocks).hasSize(1)
        assertThat(state.blocks[0]).isInstanceOf(RichTextBlockEditState::class.java)
    }

    @Test
    fun `test that mergeBlockBackward does nothing at the document start or against code`() {
        val state = stateOf(
            RichBlock.CodeBlock(null, "code"),
            RichBlock.Paragraph(RichText("after")),
        )

        assertThat(state.mergeBlockBackward(1)).isFalse()

        val single = stateOf(RichBlock.Paragraph(RichText("only")))
        assertThat(single.mergeBlockBackward(0)).isFalse()
    }

    @Test
    fun `test that cycleFocusedHeading cycles body through H1 to H3 and back`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0
        val block = state.blocks[0] as RichTextBlockEditState

        val seen = mutableListOf<RichBlockKind>()
        repeat(4) {
            state.cycleFocusedHeading()
            seen += block.kind
        }

        assertThat(seen).containsExactly(
            RichBlockKind.Heading(1),
            RichBlockKind.Heading(2),
            RichBlockKind.Heading(3),
            RichBlockKind.Paragraph,
        ).inOrder()
    }

    @Test
    fun `test that toggleFocusedListItem toggles flips and clears list kinds`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0
        val block = state.blocks[0] as RichTextBlockEditState

        state.toggleFocusedListItem(ordered = false)
        assertThat(block.kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = null))

        state.toggleFocusedListItem(ordered = true)
        assertThat(block.kind)
            .isEqualTo(RichBlockKind.Item(ordered = true, indent = 0, checked = null))

        state.toggleFocusedListItem(ordered = true)
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that toggleFocusedTaskItem cycles paragraph to task and back`() {
        val state = stateOf(RichBlock.Paragraph(RichText("todo")))
        state.focusedIndex = 0
        val block = state.blocks[0] as RichTextBlockEditState

        state.toggleFocusedTaskItem()
        assertThat(block.kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = false))

        state.toggleFocusedTaskItem()
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that toggleFocusedTaskItem adds a box to an existing list item`() {
        val state = stateOf(
            RichBlock.ListItem(ordered = true, indent = 1, checked = null, RichText("t")),
        )
        state.focusedIndex = 0

        state.toggleFocusedTaskItem()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = true, indent = 1, checked = false))
    }

    @Test
    fun `test that the list toggle sheds the task box instead of clearing the item`() {
        val state = stateOf(
            RichBlock.ListItem(ordered = false, indent = 0, checked = true, RichText("t")),
        )
        state.focusedIndex = 0

        state.toggleFocusedListItem(ordered = false)

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = null))
    }

    @Test
    fun `test that toggleFocusedTaskItem is undoable`() {
        val state = stateOf(RichBlock.Paragraph(RichText("todo")))
        state.focusedIndex = 0

        state.toggleFocusedTaskItem()
        assertThat(state.undo()).isTrue()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that toggleFocusedQuote toggles between quote and paragraph`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0
        val block = state.blocks[0] as RichTextBlockEditState

        state.toggleFocusedQuote()
        assertThat(block.kind).isEqualTo(RichBlockKind.Quote(1))

        state.toggleFocusedQuote()
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that structural toggles do nothing when no block is focused`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))

        state.cycleFocusedHeading()
        state.toggleFocusedListItem(ordered = false)
        state.toggleFocusedQuote()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that undo restores the document before a split and redo reapplies it`() {
        val state = stateOf(RichBlock.Paragraph(RichText("hello world")))

        state.splitBlock(0, 6)
        assertThat(state.blocks).hasSize(2)

        assertThat(state.undo()).isTrue()
        assertThat(state.blocks).hasSize(1)
        assertThat((state.blocks[0] as RichTextBlockEditState).text.textFieldState.text.toString())
            .isEqualTo("hello world")

        assertThat(state.redo()).isTrue()
        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that undo reverts a structural retype`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0

        state.toggleFocusedQuote()
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Quote(1))

        assertThat(state.undo()).isTrue()
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that undo steps back through typing checkpoints`() {
        val state = stateOf(RichBlock.Paragraph(RichText("a")))
        val block = state.blocks[0] as RichTextBlockEditState

        block.text.textFieldState.edit { append("b") }
        state.recordUndo()
        block.text.textFieldState.edit { append("c") }
        state.recordUndo()

        assertThat(state.undo()).isTrue()
        assertThat(block(state).text.textFieldState.text.toString()).isEqualTo("ab")
        assertThat(state.undo()).isTrue()
        assertThat(block(state).text.textFieldState.text.toString()).isEqualTo("a")
        assertThat(state.undo()).isFalse()
    }

    @Test
    fun `test that recordUndo does not stack an unchanged document`() {
        val state = stateOf(RichBlock.Paragraph(RichText("a")))

        state.recordUndo()
        state.recordUndo()

        assertThat(state.undo()).isFalse()
    }

    @Test
    fun `test that a new checkpoint clears the redo history`() {
        val state = stateOf(RichBlock.Paragraph(RichText("a")))
        val edit: (String) -> Unit = { text ->
            (state.blocks[0] as RichTextBlockEditState).text.textFieldState.edit {
                append(text)
            }
        }

        edit("b")
        state.recordUndo()
        state.undo()
        edit("x")
        state.recordUndo()

        assertThat(state.redo()).isFalse()
    }

    @Test
    fun `test that setTaskChecked updates the kind and is undoable`() {
        val state = stateOf(
            RichBlock.ListItem(ordered = false, indent = 0, checked = false, RichText("t")),
        )

        state.setTaskChecked(0, true)
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = true))

        assertThat(state.undo()).isTrue()
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = false))
    }

    @Test
    fun `test that the undo stack is capped and never crashes when exhausted`() {
        val state = stateOf(RichBlock.Paragraph(RichText("")))
        repeat(60) { index ->
            (state.blocks[0] as RichTextBlockEditState).text.textFieldState.edit {
                append("$index,")
            }
            state.recordUndo()
        }

        var undoCount = 0
        while (state.undo()) undoCount++

        assertThat(undoCount).isAtMost(50)
        assertThat(undoCount).isGreaterThan(0)
    }

    @Test
    fun `test that a no-op undo keeps the baseline snapshot undoable`() {
        val state = stateOf(RichBlock.Paragraph(RichText("original")))

        assertThat(state.undo()).isFalse()

        block(state).text.textFieldState.edit { append("!") }
        state.recordUndo()

        assertThat(state.undo()).isTrue()
        assertThat(block(state).text.textFieldState.text.toString()).isEqualTo("original")
    }

    @Test
    fun `test that a checkpoint fired after undo does not wipe the redo history`() {
        val state = stateOf(RichBlock.Paragraph(RichText("hello world")))
        state.splitBlock(0, 6)
        assertThat(state.undo()).isTrue()

        // The debounced typing collector fires after every restore (rebuilding the blocks
        // resets the edit counters it watches); it must dedup, not clear redo.
        state.recordUndo()

        assertThat(state.redo()).isTrue()
        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that typing after undo invalidates redo once checkpointed`() {
        val state = stateOf(RichBlock.Paragraph(RichText("hello world")))
        state.splitBlock(0, 6)
        state.undo()

        block(state).text.textFieldState.edit { append("!") }
        state.recordUndo()

        assertThat(state.redo()).isFalse()
    }

    @Test
    fun `test that undo redo cycles are stable across repeated presses`() {
        val state = stateOf(RichBlock.Paragraph(RichText("hello world")))
        state.splitBlock(0, 6)

        repeat(3) {
            assertThat(state.undo()).isTrue()
            assertThat(state.blocks).hasSize(1)
            assertThat(state.undo()).isFalse()
            assertThat(state.redo()).isTrue()
            assertThat(state.blocks).hasSize(2)
            assertThat(state.redo()).isFalse()
        }
    }

    private fun block(state: RichDocumentState): RichTextBlockEditState =
        state.blocks[0] as RichTextBlockEditState

    private fun stateOf(vararg blocks: RichBlock): RichDocumentState =
        RichDocumentState(RichDocument(blocks.toList()))

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
