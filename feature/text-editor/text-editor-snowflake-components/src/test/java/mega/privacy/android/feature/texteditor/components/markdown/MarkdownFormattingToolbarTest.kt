package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownFormattingToolbarTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val receivedActions = mutableListOf<MarkdownFormatAction>()
    private val receivedTextStyles = mutableListOf<Int?>()

    private fun setToolbar(
        formats: MarkdownSelectionFormats = MarkdownSelectionFormats.Empty,
        textStyleEnabled: Boolean = true,
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                MarkdownFormattingToolbar(
                    formats = formats,
                    onAction = receivedActions::add,
                    onTextStyleSelected = receivedTextStyles::add,
                    textStyleEnabled = textStyleEnabled,
                )
            }
        }
    }

    @Test
    fun `test that all formatting buttons are present`() {
        setToolbar()

        listOf(
            MarkdownFormatAction.Bold,
            MarkdownFormatAction.Italic,
            MarkdownFormatAction.Underline,
            MarkdownFormatAction.Strikethrough,
            MarkdownFormatAction.BulletList,
            MarkdownFormatAction.OrderedList,
            MarkdownFormatAction.TaskList,
            MarkdownFormatAction.Quote,
            MarkdownFormatAction.InlineCode,
            MarkdownFormatAction.Link,
        ).forEach { action ->
            // The row scrolls horizontally, so trailing buttons may be off-viewport.
            composeRule.onNodeWithTag(markdownToolbarActionTag(action)).assertExists()
        }
        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG).assertExists()
    }

    @Test
    fun `test that clicking a button emits its action`() {
        setToolbar()

        composeRule.onNodeWithTag(markdownToolbarActionTag(MarkdownFormatAction.Bold))
            .performClick()

        assertThat(receivedActions).containsExactly(MarkdownFormatAction.Bold)
    }

    @Test
    fun `test that the text style chip names the focused block style`() {
        setToolbar(formats = MarkdownSelectionFormats(headingLevel = 2))

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG)
            .assertTextEquals("Heading 2")
    }

    @Test
    fun `test that selecting a heading in the style menu emits its level`() {
        setToolbar()

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG).performClick()
        composeRule.onNodeWithTag(markdownTextStyleOptionTag(3)).performClick()

        assertThat(receivedTextStyles).containsExactly(3)
        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_MENU_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that selecting Body in the style menu emits null when a heading is active`() {
        setToolbar(formats = MarkdownSelectionFormats(headingLevel = 1))

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG).performClick()
        composeRule.onNodeWithTag(markdownTextStyleOptionTag(null)).performClick()

        assertThat(receivedTextStyles).containsExactly(null)
    }

    @Test
    fun `test that selecting the current style emits nothing`() {
        setToolbar(formats = MarkdownSelectionFormats(headingLevel = 1))

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG).performClick()
        composeRule.onNodeWithTag(markdownTextStyleOptionTag(1)).performClick()

        assertThat(receivedTextStyles).isEmpty()
    }

    @Test
    fun `test that the text style menu does not open when textStyleEnabled is false`() {
        setToolbar(textStyleEnabled = false)

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_CHIP_TAG)
            .performTouchInput { click() }

        composeRule.onNodeWithTag(MARKDOWN_TEXT_STYLE_MENU_TAG).assertDoesNotExist()
    }
}
