package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_LIST_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VideoToPlaylistView
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoToPlaylistViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        items: List<VideoPlaylistSetUiEntity> = emptyList(),
        searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
        query: String? = null,
        hasSelectedItems: Boolean = false,
        modifier: Modifier = Modifier,
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        onItemClicked: (VideoPlaylistSetUiEntity) -> Unit = {},
        onNewPlaylistClicked: () -> Unit = {},
        onDoneButtonClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoToPlaylistView(
                items = items,
                searchState = searchState,
                query = query,
                hasSelectedItems = hasSelectedItems,
                modifier = modifier,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed,
                onItemClicked = onItemClicked,
                onNewPlaylistClicked = onNewPlaylistClicked,
                onDoneButtonClicked = onDoneButtonClicked
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when the items are empty`() {
        setComposeContent()

        listOf(
            VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG,
            VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG,
            VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG,
        ).map {
            it.isDisplayed()
        }
        VIDEO_TO_PLAYLIST_LIST_TEST_TAG.doesNotExist()
    }

    private fun String.isDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.doesNotExist() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun `test that the UIs are displayed correctly when the items are not empty`() {
        setComposeContent(initTestItems())

        listOf(
            VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG,
            VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG,
            VIDEO_TO_PLAYLIST_LIST_TEST_TAG,
        ).map {
            it.isDisplayed()
        }
        (0..3).map {
            VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG + it
        }.map {
            it.isDisplayed()
        }
        VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG.doesNotExist()
    }

    private fun initTestItems() = (0..3).map {
        VideoPlaylistSetUiEntity(
            id = it.toLong(),
            title = "Video Playlist Set $it"
        )
    }

    @Test
    fun `test that onDoneButtonClicked is invoked as expected`() {
        val onDoneButtonClicked: () -> Unit = mock()
        setComposeContent(
            items = initTestItems(),
            hasSelectedItems = true,
            onDoneButtonClicked = onDoneButtonClicked
        )

        VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG.performClick()
        verify(onDoneButtonClicked).invoke()
    }

    private fun String.performClick() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).performClick()

    @Test
    fun `test that onNewPlaylistClicked is invoked as expected`() {
        val onNewPlaylistClicked: () -> Unit = mock()
        setComposeContent(onNewPlaylistClicked = onNewPlaylistClicked)

        VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG.performClick()
        verify(onNewPlaylistClicked).invoke()
    }

    @Test
    fun `test that onItemClicked is invoked as expected`() {
        val onItemClicked: (VideoPlaylistSetUiEntity) -> Unit = mock()
        val testTitle = "Video Playlist Set"
        val testSet = VideoPlaylistSetUiEntity(
            id = 1L,
            title = testTitle
        )
        setComposeContent(items = listOf(testSet), onItemClicked = onItemClicked)

        composeTestRule.onNodeWithText(text = testTitle, useUnmergedTree = true).performClick()
        verify(onItemClicked).invoke(testSet)
    }
}