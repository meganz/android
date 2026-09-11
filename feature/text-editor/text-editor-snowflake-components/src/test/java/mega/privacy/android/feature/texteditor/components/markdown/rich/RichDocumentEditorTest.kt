package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RichDocumentEditorTest {

    @get:Rule
    var composeRule = createComposeRule()

    private lateinit var state: RichDocumentState

    private fun setEditor(document: RichDocument) {
        state = RichDocumentState(document)
        composeRule.setContent {
            AndroidThemeForPreviews {
                RichDocumentEditor(state = state)
            }
        }
    }

    private fun setEditor(document: RichDocument, autoFocusWhenEmpty: Boolean) {
        state = RichDocumentState(document)
        composeRule.setContent {
            AndroidThemeForPreviews {
                RichDocumentEditor(state = state, autoFocusWhenEmpty = autoFocusWhenEmpty)
            }
        }
    }

    @Test
    fun `test that autoFocusWhenEmpty focuses the single empty block`() {
        setEditor(
            RichDocument(listOf(RichBlock.Paragraph(RichText("")))),
            autoFocusWhenEmpty = true,
        )

        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsFocused()
    }

    @Test
    fun `test that autoFocusWhenEmpty does not grab focus when the document has content`() {
        setEditor(
            RichDocument(listOf(RichBlock.Paragraph(RichText("existing")))),
            autoFocusWhenEmpty = true,
        )

        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsNotFocused()
    }

    @Test
    fun `test that an empty document is not focused by default`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("")))))

        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsNotFocused()
    }

    @Test
    fun `test that table cell text is editable in place`() {
        setEditor(RichDocument(listOf(RichBlock.RawSource("| a |\n| --- |\n| b |"))))

        composeRule.onNodeWithTag(richTableCellTag(0, 1, 0)).performTextInput("x")

        val table = state.blocks[0] as TableBlockEditState
        assertThat(table.rows[0][0].textFieldState.text.toString()).isEqualTo("bx")
    }

    @Test
    fun `test that Enter does not insert a newline in a table cell`() {
        setEditor(RichDocument(listOf(RichBlock.RawSource("| a |\n| --- |\n| b |"))))

        composeRule.onNodeWithTag(richTableCellTag(0, 1, 0)).performTextInput("\n")

        val table = state.blocks[0] as TableBlockEditState
        assertThat(table.rows[0][0].textFieldState.text.toString()).isEqualTo("b")
    }

    @Test
    fun `test that a multi-line paste into a table cell becomes single-line text`() {
        setEditor(RichDocument(listOf(RichBlock.RawSource("| a |\n| --- |\n| b |"))))

        composeRule.onNodeWithTag(richTableCellTag(0, 1, 0)).performTextInput("x\ny")

        val table = state.blocks[0] as TableBlockEditState
        assertThat(table.rows[0][0].textFieldState.text.toString()).isEqualTo("bx y")
    }

    @Test
    fun `test that focusing a table cell points focusedIndex at the table block`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.Paragraph(RichText("text")),
                    RichBlock.RawSource("| a |\n| --- |\n| b |"),
                ),
            ),
        )
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()
        assertThat(state.focusedIndex).isEqualTo(0)

        composeRule.onNodeWithTag(richTableCellTag(1, 1, 0)).requestFocus()
        composeRule.waitForIdle()

        assertThat(state.focusedIndex).isEqualTo(1)
        assertThat(state.focusedTextBlock).isNull()
        assertThat(state.focusedCell).isSameInstanceAs(
            (state.blocks[1] as TableBlockEditState).rows[0][0],
        )

        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()

        assertThat(state.focusedCell).isNull()
    }

    @Test
    fun `test that toolbar list toggle on a heading keeps the field focused`() {
        setEditor(RichDocument(listOf(RichBlock.Heading(2, RichText("title")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()

        state.toggleFocusedListItem(ordered = false)
        composeRule.waitForIdle()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = null))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsFocused()
    }

    @Test
    fun `test that heading cycle keeps the field focused across the node recreation`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("title")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()

        state.cycleFocusedHeading()
        composeRule.waitForIdle()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Heading(1))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsFocused()
    }

    @Test
    fun `test that blocks are directly editable without any reveal tap`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.Heading(1, RichText("Title")),
                    RichBlock.Paragraph(RichText("body")),
                ),
            ),
        )

        composeRule.onNodeWithTag(richBlockFieldTag(1)).performTextInput("!")
        composeRule.waitForIdle()

        val paragraph = state.toDocument().blocks[1] as RichBlock.Paragraph
        assertThat(paragraph.text.text).isEqualTo("body!")
    }

    @Test
    fun `test that list chrome renders bullets numbers and checkboxes`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(ordered = false, indent = 0, checked = null, RichText("b")),
                    RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("x")),
                    RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("y")),
                    RichBlock.ListItem(ordered = false, indent = 0, checked = false, RichText("t")),
                ),
            ),
        )

        composeRule.onNodeWithText("•  ").assertExists()
        composeRule.onNodeWithText("1. ").assertExists()
        composeRule.onNodeWithText("2. ").assertExists()
        composeRule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun `test that toggling a task checkbox updates the block kind`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(ordered = false, indent = 0, checked = false, RichText("t")),
                ),
            ),
        )

        composeRule.onNode(isToggleable()).assertIsToggleable().performClick()
        composeRule.waitForIdle()

        val item = state.toDocument().blocks[0] as RichBlock.ListItem
        assertThat(item.checked).isTrue()
    }

    @Test
    fun `test that focusing a field records the focused block index`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.Paragraph(RichText("first")),
                    RichBlock.Paragraph(RichText("second")),
                ),
            ),
        )

        composeRule.onNodeWithTag(richBlockFieldTag(1)).performTextInput("x")
        composeRule.waitForIdle()

        assertThat(state.focusedIndex).isEqualTo(1)
    }

    @Test
    fun `test that pressing Enter splits the block and focuses the new one`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("hello world")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performTextInputSelection(TextRange(6))

        composeRule.onNodeWithTag(richBlockFieldTag(0)).performTextInput("\n")
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(2)
        val first = state.blocks[0] as RichTextBlockEditState
        val second = state.blocks[1] as RichTextBlockEditState
        assertThat(first.text.textFieldState.text.toString()).isEqualTo("hello ")
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("world")
        // Both one-shot requests (split, then focus) were consumed by the editor.
        assertThat(state.pendingSplit).isNull()
        assertThat(state.pendingFocus).isNull()

        composeRule.onNodeWithTag(richBlockFieldTag(1)).performTextInput("!")
        composeRule.waitForIdle()
        assertThat(second.text.textFieldState.text.toString()).isEqualTo("!world")
    }

    @Test
    fun `test that pressing Enter on a list item continues the list`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("one")),
                ),
            ),
        )
        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performTextInputSelection(TextRange(3))

        composeRule.onNodeWithTag(richBlockFieldTag(0)).performTextInput("\n")
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(2)
        assertThat((state.blocks[1] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = true, indent = 0, checked = null))
    }

    @Test
    fun `test that backspace at the start of a block merges it into the previous one`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.Paragraph(RichText("first")),
                    RichBlock.Paragraph(RichText("second")),
                ),
            ),
        )
        composeRule.onNodeWithTag(richBlockFieldTag(1)).requestFocus()
        composeRule.onNodeWithTag(richBlockFieldTag(1))
            .performTextInputSelection(TextRange(0))

        composeRule.onNodeWithTag(richBlockFieldTag(1))
            .performKeyInput { pressKey(Key.Backspace) }
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(1)
        val merged = state.blocks[0] as RichTextBlockEditState
        assertThat(merged.text.textFieldState.text.toString()).isEqualTo("firstsecond")
    }

    @Test
    fun `test that backspace at the start of a list item removes the marker first`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(
                        ordered = false,
                        indent = 0,
                        checked = null,
                        RichText("item")
                    ),
                ),
            ),
        )
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performTextInputSelection(TextRange(0))

        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performKeyInput { pressKey(Key.Backspace) }
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(1)
        val block = state.blocks[0] as RichTextBlockEditState
        assertThat(block.kind).isEqualTo(RichBlockKind.Paragraph)
        assertThat(block.text.textFieldState.text.toString()).isEqualTo("item")
    }

    @Test
    fun `test that ctrl z undoes the last split and ctrl shift z redoes it`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("hello world")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performTextInputSelection(TextRange(6))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).performTextInput("\n")
        composeRule.waitForIdle()
        assertThat(state.blocks).hasSize(2)

        composeRule.onNodeWithTag(richBlockFieldTag(1)).performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) }
        }
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(1)
        assertThat((state.blocks[0] as RichTextBlockEditState).text.textFieldState.text.toString())
            .isEqualTo("hello world")

        composeRule.onNodeWithTag(richBlockFieldTag(0)).performKeyInput {
            withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Z) } }
        }
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that redo still works after the typing checkpoint debounce elapses`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("hello world")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0))
            .performTextInputSelection(TextRange(6))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).performTextInput("\n")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(richBlockFieldTag(1)).performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) }
        }
        composeRule.waitForIdle()
        assertThat(state.blocks).hasSize(1)

        // Let the debounced typing-checkpoint collector fire (restore resets the edit
        // counters it watches); it must not wipe the redo history.
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(richBlockFieldTag(0)).performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Y) }
        }
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(2)
    }

    @Test
    fun `test that retyping the focused block to a list keeps focus and the keyboard`() {
        setEditor(RichDocument(listOf(RichBlock.Paragraph(RichText("item")))))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()

        composeRule.runOnIdle { state.toggleFocusedListItem(ordered = false) }
        composeRule.waitForIdle()

        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Item(ordered = false, indent = 0, checked = null))
        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsFocused()
    }

    @Test
    fun `test that Enter on an empty list item exits the list without losing focus`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(ordered = false, indent = 0, checked = null, RichText("")),
                ),
            ),
        )
        composeRule.onNodeWithTag(richBlockFieldTag(0)).requestFocus()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(richBlockFieldTag(0)).performTextInput("\n")
        composeRule.waitForIdle()

        assertThat(state.blocks).hasSize(1)
        assertThat((state.blocks[0] as RichTextBlockEditState).kind)
            .isEqualTo(RichBlockKind.Paragraph)
        composeRule.onNodeWithTag(richBlockFieldTag(0)).assertIsFocused()
    }

    @Test
    fun `test that ordered items respect the run's start number`() {
        setEditor(
            RichDocument(
                listOf(
                    RichBlock.ListItem(
                        ordered = true,
                        indent = 0,
                        checked = null,
                        text = RichText("five"),
                        startNumber = 5,
                    ),
                    RichBlock.ListItem(
                        ordered = true,
                        indent = 0,
                        checked = null,
                        text = RichText("six"),
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("5. ").assertExists()
        composeRule.onNodeWithText("6. ").assertExists()
    }

    @Test
    fun `test that raw source blocks render like the preview instead of showing syntax`() {
        val table = "| City | Nights |\n|------|--------|\n| Rome | 3 |"
        setEditor(RichDocument(listOf(RichBlock.RawSource(table))))

        composeRule.onNodeWithText("City").assertExists()
        composeRule.onNodeWithText("Rome").assertExists()
        composeRule.onNodeWithText("|------|", substring = true).assertDoesNotExist()
    }
}
