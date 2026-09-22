package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownEditModeMenuTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val selections = mutableListOf<Boolean>()

    private fun setMenu(isRichText: Boolean = true) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                MarkdownEditModeMenu(
                    isRichText = isRichText,
                    onSelectRichText = selections::add,
                )
            }
        }
    }

    @Test
    fun `test that the menu is closed until the button is tapped`() {
        setMenu()

        composeRule.onNodeWithText("Plain text").assertDoesNotExist()

        composeRule.onNodeWithTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG).performClick()

        composeRule.onNodeWithText("Plain text").assertIsDisplayed()
        composeRule.onNodeWithText("Rich text").assertIsDisplayed()
    }

    @Test
    fun `test that selecting plain text reports rich text off`() {
        setMenu(isRichText = true)

        composeRule.onNodeWithTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("Plain text").performClick()

        assertThat(selections).containsExactly(false)
    }

    @Test
    fun `test that selecting rich text reports rich text on`() {
        setMenu(isRichText = false)

        composeRule.onNodeWithTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("Rich text").performClick()

        assertThat(selections).containsExactly(true)
    }

    @Test
    fun `test that only the active mode is checked`() {
        setMenu(isRichText = true)

        composeRule.onNodeWithTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG).performClick()

        composeRule.onAllNodesWithContentDescription("Selected").assertCountEquals(1)
        composeRule.onNode(hasText("Rich text") and hasContentDescription("Selected"))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the menu closes after a selection`() {
        setMenu()

        composeRule.onNodeWithTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("Plain text").performClick()

        composeRule.onNodeWithText("Plain text").assertDoesNotExist()
    }
}
