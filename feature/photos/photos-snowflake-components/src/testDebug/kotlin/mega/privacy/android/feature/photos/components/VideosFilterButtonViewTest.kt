package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideosFilterButtonViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that UI is displayed correctly and onClick are invoked as expected`() {
        val mockOnLocationClick = mock<() -> Unit>()
        val mockOnDurationClick = mock<() -> Unit>()
        composeTestRule.setContent {
            VideosFilterButtonView(
                isLocationFilterSelected = false,
                isDurationFilterSelected = false,
                locationText = "Location",
                durationText = "Duration",
                onLocationFilterClicked = mockOnLocationClick,
                onDurationFilterClicked = mockOnDurationClick,
            )
        }

        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(mockOnLocationClick).invoke()

        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(mockOnDurationClick).invoke()
    }
}