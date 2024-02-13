package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.videosection.view.playlist.Constants
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionState
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<VideoSectionViewModel>()
    private fun setComposeContent() {
        composeTestRule.setContent {
            VideoPlaylistDetailView(
                videoSectionViewModel = viewModel
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when the playlist is null`() {
        viewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    VideoSectionState()
                )
            )
        }
        setComposeContent()

        composeTestRule.onNodeWithTag(Constants.EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TITLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TITLE_TEST_TAG)
            .assertTextEquals("")
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertTextEquals("00:00:00")
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertTextEquals("no videos")
    }

    @Test
    fun `test that ui is displayed correctly when the playlist is not null`() {
        val expectedTitle = "new playlist"
        val expectedTotalDuration = "10:00:00"
        val expectedNumberOfVideos = 2
        viewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    VideoSectionState(
                        currentVideoPlaylist = VideoPlaylistUIEntity(
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
                    )
                )
            )
        }

        setComposeContent()

        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TITLE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TITLE_TEST_TAG)
            .assertTextEquals(expectedTitle)
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_TOTAL_DURATION_TEST_TAG)
            .assertTextEquals(expectedTotalDuration)
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(Constants.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG)
            .assertTextEquals("2 Videos")
    }
}