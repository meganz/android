package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
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
    fun `test that raw source blocks render their markdown verbatim`() {
        val table = "| a | b |"
        setEditor(RichDocument(listOf(RichBlock.RawSource(table))))

        composeRule.onNodeWithText(table).assertExists()
    }
}
