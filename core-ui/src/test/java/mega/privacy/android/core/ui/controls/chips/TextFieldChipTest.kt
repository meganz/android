package mega.privacy.android.core.ui.controls.chips

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextFieldChipTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that text field chip is shown enabled`() {
        composeTestRule.setContent {
            TextFieldChip(
                onTextChange = {},
                text = "1",
                isDisabled = false,
                onFocusChange = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_FIELD_CHIP).assertExists()
    }

    @Test
    fun `test that text field chip is shown disabled`() {
        composeTestRule.setContent {
            TextFieldChip(
                onTextChange = {},
                text = "1",
                isDisabled = true,
                onFocusChange = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_FIELD_CHIP).assertExists()
    }
}