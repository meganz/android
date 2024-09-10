package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenericDescriptionWithCharacterLimitTextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that ui components are shown when text field characters are within the range`() {
        var message = "This is a description with a character limit"
        val characterLimit = 120
        composeTestRule.setContent {
            GenericDescriptionWithCharacterLimitTextField(
                maxCharacterLimit = 120,
                value = message,
                modifier = Modifier
                    .padding(top = 10.dp),
                initiallyFocused = true,
                showClearIcon = true,
                onValueChange = {
                    message = it
                }, onClearText = {
                    message = ""
                }
            )
        }
        composeTestRule.onNodeWithTag(TEXT_FIELD_LIMIT_TEXT_COUNTER_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText("${message.length}/${characterLimit}"))
        composeTestRule.onNodeWithTag(TEXT_FIELD_WITH_CHARACTER_LIMIT_VIEW_TEXT_FIELD)
            .assertIsDisplayed()
            .assert(hasText(message))
        composeTestRule.onNodeWithTag(TEXT_FIELD_LIMIT_ERROR_ROW_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that ui components are shown correctly when error message is provided`() {
        var message = ""
        val errorMessage = "This field cannot be empty"
        val characterLimit = 120
        composeTestRule.setContent {
            GenericDescriptionWithCharacterLimitTextField(
                maxCharacterLimit = 120,
                errorMessage = errorMessage,
                value = message,
                modifier = Modifier
                    .padding(top = 10.dp),
                initiallyFocused = true,
                showClearIcon = true,
                onValueChange = {
                    message = it
                }, onClearText = {
                    message = ""
                }
            )
        }
        composeTestRule.onNodeWithTag(TEXT_FIELD_LIMIT_TEXT_COUNTER_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText("${message.length}/${characterLimit}"))
        composeTestRule.onNodeWithTag(TEXT_FIELD_WITH_CHARACTER_LIMIT_VIEW_TEXT_FIELD)
            .assertIsDisplayed()
            .assert(hasText(message))
        composeTestRule.onNodeWithTag(TEXT_FIELD_LIMIT_ERROR_ROW_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEXT_FIELD_LIMIT_ICON_ERROR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }
}