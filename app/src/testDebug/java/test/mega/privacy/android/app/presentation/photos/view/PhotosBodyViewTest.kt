package test.mega.privacy.android.app.presentation.photos.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.view.PhotosBodyView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@OptIn(ExperimentalPagerApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PhotosBodyViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun `test that tab titles are visible if no photos are selected`() {
        composeTestRule.setContent {
            PhotosBodyView(
                tabs = PhotosTab.values().asList(),
                selectedTab = PhotosTab.Timeline,
                pagerState = rememberPagerState(),
                onTabSelected = {},
                timelineView = {},
                albumsView = {},
                timelineViewState = TimelineViewState(selectedPhotoCount = 0),
            )
        }

        composeTestRule.onNodeWithText(R.string.tab_title_timeline).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.tab_title_album).assertIsDisplayed()
    }

    @Test
    fun `test that tab titles are hidden if a photo is selected`() {
        composeTestRule.setContent {
            PhotosBodyView(
                tabs = PhotosTab.values().asList(),
                selectedTab = PhotosTab.Timeline,
                pagerState = rememberPagerState(),
                onTabSelected = {},
                timelineView = {},
                albumsView = {},
                timelineViewState = TimelineViewState(selectedPhotoCount = 1),
            )
        }

        composeTestRule.onNodeWithText(R.string.tab_title_timeline).assertDoesNotExist()
        composeTestRule.onNodeWithText(R.string.tab_title_album).assertDoesNotExist()
    }
}