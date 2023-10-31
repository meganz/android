package mega.privacy.android.legacy.core.ui.controls.chips

import androidx.compose.ui.Modifier
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
    fun `test that text button chip is shown checked`() {
        composeTestRule.setContent {
            TextButtonChip(
                onClick = { },
                text = "M",
                modifier = Modifier,
                isChecked = true,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).assertExists()
    }

    @Test
    fun `test that text button chip is not shown checked`() {
        composeTestRule.setContent {
            TextButtonChip(
                onClick = { },
                text = "M",
                modifier = Modifier,
                isChecked = false,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).assertExists()
    }

    @Test
    fun `test that on click event is fired when text button chip is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            TextButtonChip(
                onClick = mock,
                text = "M",
                isChecked = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_TEXT_BUTTON_CHIP).performClick()
        verify(mock).invoke()
    }
}