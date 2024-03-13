package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
        isLocationFilterSelected: Boolean = false,
        isDurationFilterSelected: Boolean = false,
        locationDefaultText: String = "",
        durationDefaultText: String = "",
        locationFilterSelectText: String = "",
        durationFilterSelectText: String = "",
        modifier: Modifier = Modifier,
        onLocationFilterClicked: () -> Unit = {},
        onDurationFilterClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideosFilterButtonView(
                isLocationFilterSelected = isLocationFilterSelected,
                isDurationFilterSelected = isDurationFilterSelected,
                locationDefaultText = locationDefaultText,
                durationDefaultText = durationDefaultText,
                locationFilterSelectText = locationFilterSelectText,
                durationFilterSelectText = durationFilterSelectText,
                modifier = modifier,
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
        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertTextEquals(testLocationDefaultText)
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
        composeTestRule.onNodeWithTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertTextEquals(testLocationSelectedText)
        composeTestRule.onNodeWithContentDescription(
            label = "Location filter selected icon",
            useUnmergedTree = true
        )
            .assertIsDisplayed()
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
        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertTextEquals(testDurationDefaultText)
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
        composeTestRule.onNodeWithTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG, true)
            .assertTextEquals(testDurationSelectedText)
        composeTestRule.onNodeWithContentDescription(
            label = "Duration filter selected icon",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that onLocationFilterClicked is invoked when location filter is clicked`() {
        val onLocationFilterClicked: () -> Unit = Mockito.mock()
        setComposeContent(onLocationFilterClicked = onLocationFilterClicked)

        composeTestRule.onNodeWithContentDescription("Location filter chip").performClick()
        verify(onLocationFilterClicked).invoke()
    }

    @Test
    fun `test that onDurationFilterClicked is invoked when duration filter is clicked`() {
        val onDurationFilterClicked: () -> Unit = Mockito.mock()
        setComposeContent(onDurationFilterClicked = onDurationFilterClicked)

        composeTestRule.onNodeWithContentDescription("Duration filter chip").performClick()
        verify(onDurationFilterClicked).invoke()
    }
}