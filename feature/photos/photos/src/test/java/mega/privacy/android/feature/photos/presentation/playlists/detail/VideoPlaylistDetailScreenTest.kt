package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: VideoPlaylistDetailUiState = VideoPlaylistDetailUiState.Data(),
        onBack: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailScreen(
                uiState = uiState,
                onBack = onBack,
                modifier = modifier,
            )
        }
    }

    @Test
    fun `test that loading view is displayed as expected`() {
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Loading
        )

        listOf(
            VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG
        ).assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected when currentPlaylist is null`() {
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Data(
                playlistDetail = null
            )
        )

        listOf(
            VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG
        ).assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected when videos is empty`() {
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Data(
                playlistDetail = mock<VideoPlaylistDetailUiEntity> {
                    on { videos }.thenReturn(emptyList())
                }
            )
        )

        VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that playlist detail view is displayed as expected`() {
        val videos = listOf(
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("Video")
                on { duration }.thenReturn(10.seconds)
                on { durationString }.thenReturn("00:10")
            }
        )
        val videoPlaylistEntity = mock<VideoPlaylistUiEntity> {
            on { thumbnailList }.thenReturn(emptyList())
        }
        val playlistDetail = mock<VideoPlaylistDetailUiEntity> {
            on { uiEntity }.thenReturn(videoPlaylistEntity)
            on { this.videos }.thenReturn(videos)
        }
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Data(
                playlistDetail = playlistDetail
            )
        )

        listOf(
            VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG
        ).assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG,
            VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun List<String>.assertIsDisplayedWithTag() =
        onEach {
            it.assertIsDisplayedWithTag()
        }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }
}