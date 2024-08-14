package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
    fun `test that ui components are shown`() {
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
    }
}