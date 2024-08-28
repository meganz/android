package mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.view.allvideos.DURATION_FILTER_BUTTON_TEXT_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.LOCATION_FILTER_BUTTON_TEXT_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideosFilterButtonView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideosFilterButtonViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        modifier: Modifier = Modifier,
        isLocationFilterSelected: Boolean = false,
        isDurationFilterSelected: Boolean = false,
        locationDefaultText: String = "",
        durationDefaultText: String = "",
        locationFilterSelectText: String = "",
        durationFilterSelectText: String = "",
        onLocationFilterClicked: () -> Unit = {},
        onDurationFilterClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideosFilterButtonView(
                modifier = modifier,
                isLocationFilterSelected = isLocationFilterSelected,
                isDurationFilterSelected = isDurationFilterSelected,
                locationDefaultText = locationDefaultText,
                durationDefaultText = durationDefaultText,
                locationFilterSelectText = locationFilterSelectText,
                durationFilterSelectText = durationFilterSelectText,
                onLocationFilterClicked = onLocationFilterClicked,
                onDurationFilterClicked = onDurationFilterClicked
            )
        }
    }

    @Test
    fun `test that location filter text is displayed correctly when filter is not selected`() {
        val testLocationDefaultText = "Location default text"
        val testLocationSelectedText = "Location selected text"
        setComposeContent(
            locationDefaultText = testLocationDefaultText,
            locationFilterSelectText = testLocationSelectedText
        )

        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(text = testLocationDefaultText, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that location filter text is displayed correctly when filter is selected`() {
        val testLocationDefaultText = "Location default text"
        val testLocationSelectedText = "Location selected text"
        setComposeContent(
            isLocationFilterSelected = true,
            locationDefaultText = testLocationDefaultText,
            locationFilterSelectText = testLocationSelectedText
        )

        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(text = testLocationSelectedText, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            label = "Leading icon",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that duration filter text is displayed correctly when filter is not selected`() {
        val testDurationDefaultText = "Duration default text"
        val testDurationSelectedText = "Duration selected text"
        setComposeContent(
            durationDefaultText = testDurationDefaultText,
            durationFilterSelectText = testDurationSelectedText
        )

        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(testDurationDefaultText, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that duration filter text is displayed correctly when filter is selected`() {
        val testDurationDefaultText = "Duration default text"
        val testDurationSelectedText = "Duration selected text"
        setComposeContent(
            isDurationFilterSelected = true,
            durationDefaultText = testDurationDefaultText,
            durationFilterSelectText = testDurationSelectedText
        )

        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(testDurationSelectedText, useUnmergedTree = true)
        composeTestRule.onNodeWithContentDescription(
            label = "Leading icon",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that onLocationFilterClicked is invoked when location filter is clicked`() {
        val onLocationFilterClicked: () -> Unit = Mockito.mock()
        setComposeContent(onLocationFilterClicked = onLocationFilterClicked)

        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true).performClick()
        verify(onLocationFilterClicked).invoke()
    }

    @Test
    fun `test that onDurationFilterClicked is invoked when duration filter is clicked`() {
        val onDurationFilterClicked: () -> Unit = Mockito.mock()
        setComposeContent(onDurationFilterClicked = onDurationFilterClicked)

        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true).performClick()
        verify(onDurationFilterClicked).invoke()
    }
}