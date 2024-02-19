package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TOTAL_DURATION_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private fun setComposeContent(playlist: VideoPlaylistUIEntity?) {
        composeTestRule.setContent {
            VideoPlaylistDetailView(playlist)
        }
    }

    @Test
    fun `test that ui is displayed correctly when the playlist is null`() {
        setComposeContent(null)

        composeTestRule.onNodeWithTag(VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_TITLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_TITLE_TEST_TAG)
            .assertTextEquals("")
        composeTestRule.onNodeWithTag(PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertTextEquals("00:00:00")
        composeTestRule.onNodeWithTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertTextEquals("no videos")
    }

    @Test
    fun `test that ui is displayed correctly when the playlist is not null`() {
        val expectedTitle = "new playlist"
        val expectedTotalDuration = "10:00:00"
        val expectedNumberOfVideos = 2

        val playlist = VideoPlaylistUIEntity(
            id = NodeId(1L),
            title = expectedTitle,
            cover = null,
            creationTime = 0,
            modificationTime = 0,
            thumbnailList = null,
            numberOfVideos = expectedNumberOfVideos,
            totalDuration = expectedTotalDuration,
            videos = emptyList()
        )

        setComposeContent(playlist)

        composeTestRule.onNodeWithTag(PLAYLIST_TITLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_TITLE_TEST_TAG)
            .assertTextEquals(expectedTitle)
        composeTestRule.onNodeWithTag(PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertTextEquals(expectedTotalDuration)
        composeTestRule.onNodeWithTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertTextEquals("2 Videos")
    }
}