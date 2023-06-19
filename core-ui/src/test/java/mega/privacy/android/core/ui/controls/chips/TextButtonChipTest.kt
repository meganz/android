package mega.privacy.android.core.ui.controls.chips

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class TextButtonChipTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that text field chip is shown checked`() {
        composeTestRule.setContent {
            TextButtonChip(
                onClick = { },
                text = "weekdays",
                isChecked = true,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).assertExists()
    }

    @Test
    fun `test that text field chip is not shown checked`() {
        composeTestRule.setContent {
            TextButtonChip(
                onClick = { },
                text = "weekdays",
                isChecked = false,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).assertExists()
    }

    @Test
    fun `test that on click event is fired when accept icon is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            TextButtonChip(
                onClick = mock,
                text = "weekdays",
                isChecked = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).performClick()
        verify(mock).invoke()
    }

    @Test
    fun `test that icon is not shown`() {
        composeTestRule.setContent {
            TextButtonChip(
                onClick = {},
                text = "weekdays",
                isChecked = false,
                iconId = null
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_ICON).assertDoesNotExist()
    }
}