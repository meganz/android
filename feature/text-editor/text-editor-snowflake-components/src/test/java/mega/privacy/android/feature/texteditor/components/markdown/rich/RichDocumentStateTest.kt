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
    fun `test that setFocusedHeading retypes the block to the selected level and back to body`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0
        val block = state.blocks[0] as RichTextBlockEditState

        state.setFocusedHeading(2)
        assertThat(block.kind).isEqualTo(RichBlockKind.Heading(2))

        state.setFocusedHeading(3)
        assertThat(block.kind).isEqualTo(RichBlockKind.Heading(3))

        state.setFocusedHeading(null)
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
    }

    @Test
    fun `test that setFocusedHeading does not record an undo step when the level is unchanged`() {
        val state = stateOf(RichBlock.Paragraph(RichText("text")))
        state.focusedIndex = 0

        state.setFocusedHeading(null)

        assertThat(state.undo()).isFalse()
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

        state.setFocusedHeading(1)
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

    @Test
    fun `test that RichDocSelection between orders positions across blocks`() {
        val a = RichDocPosition(2, 1)
        val b = RichDocPosition(0, 5)

        assertThat(RichDocSelection.between(a, b)).isEqualTo(RichDocSelection(b, a))
        assertThat(RichDocSelection.between(b, a)).isEqualTo(RichDocSelection(b, a))
    }

    @Test
    fun `test that documentSelectionIn returns partial ranges on edge blocks and full ranges between`() {
        val state = RichDocumentState(document)
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(2, 2),
        )

        assertThat(state.documentSelectionIn(0)).isEqualTo(TextRange(2, 5))
        assertThat(state.documentSelectionIn(1)).isEqualTo(TextRange(0, 4))
        assertThat(state.documentSelectionIn(2)).isEqualTo(TextRange(0, 2))
        assertThat(state.documentSelectionIn(3)).isNull()
    }

    @Test
    fun `test that documentSelectionIn returns null when there is no selection or the block cannot select`() {
        val state = RichDocumentState(document)

        assertThat(state.documentSelectionIn(0)).isNull()

        state.documentSelection = RichDocSelection(
            RichDocPosition(5, 0),
            RichDocPosition(7, 0),
        )

        assertThat(state.documentSelectionIn(6)).isNull()
        assertThat(state.documentSelectionIn(7)).isNull()
    }

    @Test
    fun `test that deleteDocumentSelection removes edge slices and whole interior blocks`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first line", listOf(RichSpan(0, 5, RichSpanStyle.Bold)))),
            RichBlock.RawSource("| a |"),
            RichBlock.Paragraph(
                RichText(
                    "last line",
                    listOf(RichSpan(5, 9, RichSpanStyle.Italic))
                )
            ),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 5),
            RichDocPosition(2, 5),
        )

        assertThat(state.deleteDocumentSelection()).isTrue()

        assertThat(state.blocks).hasSize(1)
        val merged = state.blocks[0] as RichTextBlockEditState
        assertThat(merged.text.textFieldState.text.toString()).isEqualTo("firstline")
        assertThat(merged.text.spans).containsExactly(
            RichSpan(0, 5, RichSpanStyle.Bold),
            RichSpan(5, 9, RichSpanStyle.Italic),
        )
        assertThat(state.documentSelection).isNull()
        assertThat(state.pendingFocus).isEqualTo(RichFocusRequest(0, TextRange(5)))
    }

    @Test
    fun `test that deleteDocumentSelection deletes within one block and adjusts spans`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("abcdef", listOf(RichSpan(2, 6, RichSpanStyle.Bold)))),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 1),
            RichDocPosition(0, 4),
        )

        assertThat(state.deleteDocumentSelection()).isTrue()

        val block = state.blocks[0] as RichTextBlockEditState
        assertThat(block.text.textFieldState.text.toString()).isEqualTo("aef")
        assertThat(block.text.spans).containsExactly(RichSpan(1, 3, RichSpanStyle.Bold))
    }

    @Test
    fun `test that deleteDocumentSelection keeps a code remnant as its own block`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("intro")),
            RichBlock.CodeBlock("kotlin", "val x = 1"),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(1, 4),
        )

        assertThat(state.deleteDocumentSelection()).isTrue()

        assertThat(state.blocks).hasSize(2)
        val paragraph = state.blocks[0] as RichTextBlockEditState
        val code = state.blocks[1] as CodeBlockEditState
        assertThat(paragraph.text.textFieldState.text.toString()).isEqualTo("in")
        assertThat(code.code.text.toString()).isEqualTo("x = 1")
    }

    @Test
    fun `test that replaceDocumentSelection deletes the selection and inserts the typed text`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first line")),
            RichBlock.Paragraph(RichText("last line")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 6),
            RichDocPosition(1, 4),
        )

        assertThat(state.replaceDocumentSelection("X")).isTrue()

        assertThat(state.blocks).hasSize(1)
        val merged = state.blocks[0] as RichTextBlockEditState
        assertThat(merged.text.textFieldState.text.toString()).isEqualTo("first X line")
        assertThat(state.pendingFocus).isEqualTo(RichFocusRequest(0, TextRange(7)))
    }

    @Test
    fun `test that replaceDocumentSelection splits multi-line inserted text into blocks`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first line")),
            RichBlock.Paragraph(RichText("last line")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 6),
            RichDocPosition(1, 5),
        )

        assertThat(state.replaceDocumentSelection("one\ntwo\nthree")).isTrue()

        assertThat(state.blocks).hasSize(3)
        val texts = state.blocks.map {
            (it as RichTextBlockEditState).text.textFieldState.text.toString()
        }
        assertThat(texts).containsExactly("first one", "two", "threeline").inOrder()
        assertThat(state.pendingFocus).isEqualTo(RichFocusRequest(2, TextRange(5)))
    }

    @Test
    fun `test that replaceDocumentSelection splits at the join when the typed text is a newline`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first line")),
            RichBlock.Paragraph(RichText("last line")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 5),
            RichDocPosition(1, 5),
        )

        assertThat(state.replaceDocumentSelection("\n")).isTrue()

        assertThat(state.blocks).hasSize(2)
        val first = state.blocks[0] as RichTextBlockEditState
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(first.text.textFieldState.text.toString()).isEqualTo("first")
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("line")
    }

    @Test
    fun `test that deleteDocumentSelection returns false when there is no selection`() {
        val state = RichDocumentState(document)

        assertThat(state.deleteDocumentSelection()).isFalse()

        state.documentSelection = RichDocSelection(
            RichDocPosition(1, 2),
            RichDocPosition(1, 2),
        )

        assertThat(state.deleteDocumentSelection()).isFalse()
    }

    @Test
    fun `test that undo restores the document after deleteDocumentSelection`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first")),
            RichBlock.Paragraph(RichText("second")),
        )
        val before = state.toDocument()
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(1, 3),
        )
        state.deleteDocumentSelection()

        assertThat(state.undo()).isTrue()

        assertThat(state.toDocument()).isEqualTo(before)
    }

    @Test
    fun `test that selectedDocument slices edge blocks with their spans and kinds`() {
        val state = stateOf(
            RichBlock.Heading(
                2,
                RichText("Title here", listOf(RichSpan(0, 5, RichSpanStyle.Bold)))
            ),
            RichBlock.RawSource("| a |"),
            RichBlock.ListItem(
                ordered = false, indent = 0, checked = null,
                text = RichText("item text", listOf(RichSpan(0, 4, RichSpanStyle.Italic))),
            ),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(2, 4),
        )

        assertThat(state.selectedDocument()).isEqualTo(
            RichDocument(
                listOf(
                    RichBlock.Heading(
                        2,
                        RichText("tle here", listOf(RichSpan(0, 3, RichSpanStyle.Bold)))
                    ),
                    RichBlock.RawSource("| a |"),
                    RichBlock.ListItem(
                        ordered = false, indent = 0, checked = null,
                        text = RichText("item", listOf(RichSpan(0, 4, RichSpanStyle.Italic))),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `test that replaceDocumentSelection applies markdown structure from pasted text`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("before after")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 7),
            RichDocPosition(0, 12),
        )

        assertThat(state.replaceDocumentSelection("**bold**\n\n# Heading")).isTrue()

        assertThat(state.blocks).hasSize(2)
        val first = state.blocks[0] as RichTextBlockEditState
        assertThat(first.kind).isEqualTo(RichBlockKind.Paragraph)
        assertThat(first.text.textFieldState.text.toString()).isEqualTo("before bold")
        assertThat(first.text.spans).containsExactly(RichSpan(7, 11, RichSpanStyle.Bold))
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(second.kind).isEqualTo(RichBlockKind.Heading(1))
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("Heading")
    }

    @Test
    fun `test that smartInsert splices markdown blocks and rejoins the tail`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("head tail")),
        )

        state.smartInsert(0, 5, 5, "- one\n- two\n")

        val texts = state.blocks.map {
            (it as RichTextBlockEditState).text.textFieldState.text.toString()
        }
        assertThat(texts).containsExactly("head ", "one", "two", "tail").inOrder()
        val items = state.blocks.drop(1).take(2).map { (it as RichTextBlockEditState).kind }
        assertThat(items).containsExactly(
            RichBlockKind.Item(ordered = false, indent = 0, checked = null),
            RichBlockKind.Item(ordered = false, indent = 0, checked = null),
        )
    }

    @Test
    fun `test that smartInsert falls back to plain line splitting when there is no structure`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("head tail")),
        )

        state.smartInsert(0, 4, 5, "one\ntwo")

        val texts = state.blocks.map {
            (it as RichTextBlockEditState).text.textFieldState.text.toString()
        }
        assertThat(texts).containsExactly("headone", "twotail").inOrder()
    }

    @Test
    fun `test that moveSelectionEdge moves an edge across blocks`() {
        val state = RichDocumentState(document)
        state.documentSelection = RichDocSelection(
            RichDocPosition(1, 1),
            RichDocPosition(1, 3),
        )

        state.moveSelectionEdge(isStart = false, position = RichDocPosition(4, 2))
        state.moveSelectionEdge(isStart = true, position = RichDocPosition(0, 3))

        assertThat(state.documentSelection).isEqualTo(
            RichDocSelection(
                RichDocPosition(0, 3),
                RichDocPosition(4, 2),
            ),
        )
    }

    @Test
    fun `test that moveSelectionEdge ignores a move that would cross the other edge`() {
        val state = RichDocumentState(document)
        val selection = RichDocSelection(
            RichDocPosition(1, 1),
            RichDocPosition(2, 2),
        )
        state.documentSelection = selection

        state.moveSelectionEdge(isStart = true, position = RichDocPosition(2, 2))
        state.moveSelectionEdge(isStart = false, position = RichDocPosition(1, 0))

        assertThat(state.documentSelection).isEqualTo(selection)
    }

    @Test
    fun `test that selectAllBlocks spans the first to the last selectable block`() {
        val state = RichDocumentState(document)

        state.selectAllBlocks()

        assertThat(state.documentSelection).isEqualTo(
            RichDocSelection(
                RichDocPosition(0, 0),
                RichDocPosition(5, "val x = 1".length),
            ),
        )
    }

    @Test
    fun `test that selectedText joins edge slices with whole interior blocks including raw source`() {
        val state = RichDocumentState(document)
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(5, 3),
        )

        assertThat(state.selectedText()).isEqualTo(
            "tle\nbody\none\ntask\nquoted\nval",
        )
    }

    @Test
    fun `test that selectedText includes trailing raw source when the selection ends past it`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("intro")),
            RichBlock.RawSource("| a |\n|---|"),
            RichBlock.Paragraph(RichText("outro")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 0),
            RichDocPosition(2, 5),
        )

        assertThat(state.selectedText()).isEqualTo("intro\n| a |\n|---|\noutro")
    }

    @Test
    fun `test that selectedText returns null when the selection is collapsed`() {
        val state = RichDocumentState(document)
        state.documentSelection = RichDocSelection(
            RichDocPosition(1, 2),
            RichDocPosition(1, 2),
        )

        assertThat(state.selectedText()).isNull()
    }

    @Test
    fun `test that blockTextLength reports text and code lengths and null elsewhere`() {
        val state = RichDocumentState(document)

        assertThat(state.blockTextLength(0)).isEqualTo(5)
        assertThat(state.blockTextLength(5)).isEqualTo("val x = 1".length)
        assertThat(state.blockTextLength(6)).isNull()
        assertThat(state.blockTextLength(7)).isNull()
        assertThat(state.blockTextLength(99)).isNull()
    }

    @Test
    fun `test that an untouched table serializes back to its verbatim raw source`() {
        val source = "| Name |   Age |\n| :--- | ----: |\n| Ada  |    36 |"
        val state = stateOf(RichBlock.RawSource(source))

        val table = state.blocks[0]
        assertThat(table).isInstanceOf(TableBlockEditState::class.java)
        assertThat(state.toDocument().blocks[0]).isEqualTo(RichBlock.RawSource(source))
    }

    @Test
    fun `test that editing a table cell produces a table block that serializes normalized`() {
        val source = "| Name | Age |\n| :--- | ---: |\n| **Ada** | 36 |"
        val state = stateOf(RichBlock.RawSource(source))

        val table = state.blocks[0] as TableBlockEditState
        table.rows[0][1].textFieldState.edit { replace(0, length, "37") }

        val block = state.toDocument().blocks[0] as RichBlock.Table
        assertThat(block.rows[0][1].text).isEqualTo("37")
        assertThat(block.rows[0][0]).isEqualTo(
            RichText("Ada", listOf(RichSpan(0, 3, RichSpanStyle.Bold))),
        )
        assertThat(RichDocumentToMarkdownConverter().convert(RichDocument(listOf(block))))
            .isEqualTo("| Name | Age |\n| :--- | ---: |\n| **Ada** | 37 |")
    }

    @Test
    fun `test that a table with an image cell stays read-only raw source`() {
        val source = "| a |\n| --- |\n| ![img](x.png) |"
        val state = stateOf(RichBlock.RawSource(source))

        assertThat(state.blocks[0]).isInstanceOf(RawSourceEditState::class.java)
    }

    @Test
    fun `test that a pipe typed into a cell is escaped when serialized`() {
        val source = "| a |\n| --- |\n| b |"
        val state = stateOf(RichBlock.RawSource(source))
        val table = state.blocks[0] as TableBlockEditState
        table.rows[0][0].textFieldState.edit { replace(0, length, "x | y") }

        val markdown = RichDocumentToMarkdownConverter().convert(state.toDocument())

        assertThat(markdown).isEqualTo("| a |\n| --- |\n| x \\| y |")
    }

    @Test
    fun `test that focusedInlineText prefers the focused cell over the focused block`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("text")),
            RichBlock.RawSource("| a |\n| --- |\n| b |"),
        )
        state.focusedIndex = 0
        assertThat(state.focusedInlineText).isSameInstanceAs(
            (state.blocks[0] as RichTextBlockEditState).text,
        )

        val cell = (state.blocks[1] as TableBlockEditState).rows[0][0]
        state.focusedIndex = 1
        state.focusedCell = cell

        assertThat(state.focusedInlineText).isSameInstanceAs(cell)
        assertThat(state.focusedTextBlock).isNull()
    }

    @Test
    fun `test that undo clears the focused cell`() {
        val state = stateOf(RichBlock.RawSource("| a |\n| --- |\n| b |"))
        val table = state.blocks[0] as TableBlockEditState
        state.focusedCell = table.rows[0][0]
        table.rows[0][0].textFieldState.edit { append("!") }
        state.recordUndo()

        state.undo()

        assertThat(state.focusedCell).isNull()
    }

    @Test
    fun `test that toggleStyleOverDocumentSelection applies the style across blocks`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first")),
            RichBlock.Paragraph(RichText("second")),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 2),
            RichDocPosition(1, 3),
        )

        assertThat(state.toggleStyleOverDocumentSelection(RichSpanStyle.Bold)).isTrue()

        assertThat(block(state, 0).text.spans)
            .containsExactly(RichSpan(2, 5, RichSpanStyle.Bold))
        assertThat(block(state, 1).text.spans)
            .containsExactly(RichSpan(0, 3, RichSpanStyle.Bold))
        assertThat(state.documentSelection).isNotNull()
    }

    @Test
    fun `test that toggleStyleOverDocumentSelection removes the style when fully covered`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first", listOf(RichSpan(0, 5, RichSpanStyle.Bold)))),
            RichBlock.Paragraph(RichText("second", listOf(RichSpan(0, 6, RichSpanStyle.Bold)))),
        )
        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 0),
            RichDocPosition(1, 6),
        )

        state.toggleStyleOverDocumentSelection(RichSpanStyle.Bold)

        assertThat(block(state, 0).text.spans).isEmpty()
        assertThat(block(state, 1).text.spans).isEmpty()
    }

    @Test
    fun `test that documentSelectionStyleCovered reports coverage across the selection`() {
        val state = stateOf(
            RichBlock.Paragraph(RichText("first", listOf(RichSpan(0, 5, RichSpanStyle.Bold)))),
            RichBlock.Paragraph(RichText("second", listOf(RichSpan(0, 3, RichSpanStyle.Bold)))),
        )

        assertThat(state.documentSelectionStyleCovered(RichSpanStyle.Bold)).isNull()

        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 0),
            RichDocPosition(1, 3),
        )
        assertThat(state.documentSelectionStyleCovered(RichSpanStyle.Bold)).isTrue()

        state.documentSelection = RichDocSelection(
            RichDocPosition(0, 0),
            RichDocPosition(1, 6),
        )
        assertThat(state.documentSelectionStyleCovered(RichSpanStyle.Bold)).isFalse()
    }

    private fun block(state: RichDocumentState, index: Int): RichTextBlockEditState =
        state.blocks[index] as RichTextBlockEditState

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
