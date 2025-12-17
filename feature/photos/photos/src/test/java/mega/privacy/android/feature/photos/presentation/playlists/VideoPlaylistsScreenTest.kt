package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: VideoPlaylistsTabUiState = VideoPlaylistsTabUiState.Data(),
        modifier: Modifier = Modifier
    ) {
        composeTestRule.setContent {
            VideoPlaylistsTabScreen(
                uiState = uiState,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that loading view is displayed as expected`() {
        setComposeContent(
            uiState = VideoPlaylistsTabUiState.Loading
        )

        VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected`() {
        setComposeContent(
            uiState = VideoPlaylistsTabUiState.Data(
                videoPlaylistEntities = emptyList()
            )
        )

        VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that all videos view is displayed as expected`() {
        val video = createVideoPlaylistUiEntity(1L)
        setComposeContent(
            uiState = VideoPlaylistsTabUiState.Data(
                videoPlaylistEntities = listOf(video)
            )
        )

        VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()


    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun createVideoPlaylistUiEntity(
        handle: Long,
        title: String = "Video playlist $handle",
    ) = mock<VideoPlaylistUiEntity> {
        on { id }.thenReturn(NodeId(handle))
        on { this.title }.thenReturn(title)
    }
}