package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.nodecomponents.list.SORT_ORDER_TAG
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
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
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        composeTestRule.setContent {
            VideoPlaylistsTabScreen(
                uiState = uiState,
                onSortNodes = onSortNodes,
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

    @Test
    fun `test that SortBottomSheet is displayed correctly`() {
        val video = createVideoPlaylistUiEntity(1L)
        val onSortNodes = mock<(NodeSortConfiguration) -> Unit>()
        setComposeContent(
            uiState = VideoPlaylistsTabUiState.Data(
                videoPlaylistEntities = listOf(video)
            ),
            onSortNodes = onSortNodes
        )

        VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        with(SORT_ORDER_TAG.getNodeWithTag()) {
            assertIsDisplayed()
            performClick()
        }

        VIDEO_PLAYLISTS_TAB_SORT_BOTTOM_SHEET_TEST_TAG.assertIsDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()


    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun String.getNodeWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)

    private fun createVideoPlaylistUiEntity(
        handle: Long,
        title: String = "Video playlist $handle",
    ) = mock<VideoPlaylistUiEntity> {
        on { id }.thenReturn(NodeId(handle))
        on { this.title }.thenReturn(title)
    }
}