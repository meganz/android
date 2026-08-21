package mega.privacy.android.feature.texteditor.components.markdown

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

    private fun setToolbar(
        formats: MarkdownSelectionFormats = MarkdownSelectionFormats.Empty,
        showModeSwitch: Boolean = false,
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                MarkdownFormattingToolbar(
                    formats = formats,
                    onAction = receivedActions::add,
                    showModeSwitch = showModeSwitch,
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
            MarkdownFormatAction.Strikethrough,
            MarkdownFormatAction.HeadingCycle,
            MarkdownFormatAction.BulletList,
            MarkdownFormatAction.OrderedList,
            MarkdownFormatAction.Quote,
            MarkdownFormatAction.InlineCode,
            MarkdownFormatAction.Link,
        ).forEach { action ->
            // The row scrolls horizontally, so trailing buttons may be off-viewport.
            composeRule.onNodeWithTag(markdownToolbarActionTag(action)).assertExists()
        }
    }

    @Test
    fun `test that clicking a button emits its action`() {
        setToolbar()

        composeRule.onNodeWithTag(markdownToolbarActionTag(MarkdownFormatAction.Bold))
            .performClick()

        assertThat(receivedActions).containsExactly(MarkdownFormatAction.Bold)
    }

    @Test
    fun `test that the mode switch is hidden by default`() {
        setToolbar()

        composeRule
            .onNodeWithTag(markdownToolbarActionTag(MarkdownFormatAction.SwitchEditMode))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the mode switch emits its action when shown`() {
        setToolbar(showModeSwitch = true)

        composeRule
            .onNodeWithTag(markdownToolbarActionTag(MarkdownFormatAction.SwitchEditMode))
            .performClick()

        assertThat(receivedActions).containsExactly(MarkdownFormatAction.SwitchEditMode)
    }
}
