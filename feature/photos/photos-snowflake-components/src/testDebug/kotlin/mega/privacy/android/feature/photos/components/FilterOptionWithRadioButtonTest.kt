package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class FilterOptionWithRadioButtonTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testTitle = "Option Title"

    private fun setComposeContent(
        title: String = testTitle,
        selected: Boolean = false,
        onClick: (() -> Unit)?,
    ) {
        composeTestRule.setContent {
            FilterOptionWithRadioButton(
                title = title,
                selected = selected,
                onClick = onClick,
            )
        }
    }

    @Test
    fun `test that the UI is displayed correctly and onClick is invoked as expected`() {
        val mockOnClick = mock<() -> Unit>()
        setComposeContent(onClick = mockOnClick)

        composeTestRule.onNodeWithTag(FILTER_OPTION_RADIO_BUTTON_TEST_TAG, true).assertIsDisplayed()

        composeTestRule.onNodeWithTag(FILTER_OPTION_TITLE_TEST_TAG, true).apply {
            assertIsDisplayed()
            assertTextEquals(testTitle)
        }
        composeTestRule.onNodeWithTag(FILTER_OPTION_WITH_RADIO_BUTTON_TEST_TAG, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(mockOnClick).invoke()
    }
}