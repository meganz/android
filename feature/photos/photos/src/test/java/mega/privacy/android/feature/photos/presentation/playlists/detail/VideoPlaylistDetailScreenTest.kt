package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistEditState
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
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
        showRenameVideoPlaylistDialog: () -> Unit = {},
        numberOfAddedVideos: Int? = null,
        videoPlaylistEditState: VideoPlaylistEditState = VideoPlaylistEditState(),
        updatedVideoPlaylistTitle: (NodeId, String) -> Unit = { _, _ -> },
        resetErrorMessage: () -> Unit = {},
        resetShowRenameVideoPlaylistDialog: () -> Unit = {},
        resetUpdateTitleSuccessEvent: () -> Unit = {},
        onDeleteButtonClicked: (Set<VideoPlaylistUiEntity>) -> Unit = {},
        onConsumedPlaylistRemovedEvent: () -> Unit = {},
        onClick: (VideoUiEntity) -> Unit = {},
        onLongClick: (VideoUiEntity) -> Unit = {},
        selectAll: () -> Unit = {},
        clearSelection: () -> Unit = {},
        removeVideosFromPlaylist: (List<Long>) -> Unit = {},
        resetRemoveVideosEvent: () -> Unit = {},
        snackBarQueue: SnackbarEventQueue = mock(),
        onBack: () -> Unit = {},
        onMenuClick: (NavKey) -> Unit = {},
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailScreen(
                uiState = uiState,
                onBack = onBack,
                modifier = modifier,
                showRenameVideoPlaylistDialog = showRenameVideoPlaylistDialog,
                videoPlaylistEditState = videoPlaylistEditState,
                updatedVideoPlaylistTitle = updatedVideoPlaylistTitle,
                resetErrorMessage = resetErrorMessage,
                resetShowRenameVideoPlaylistDialog = resetShowRenameVideoPlaylistDialog,
                resetUpdateTitleSuccessEvent = resetUpdateTitleSuccessEvent,
                onDeleteButtonClicked = onDeleteButtonClicked,
                onConsumedPlaylistRemovedEvent = onConsumedPlaylistRemovedEvent,
                onClick = onClick,
                onLongClick = onLongClick,
                selectAll = selectAll,
                clearSelection = clearSelection,
                multiNodeActionHandler = mock(),
                snackBarQueue = snackBarQueue,
                numberOfAddedVideos = numberOfAddedVideos,
                removeVideosFromPlaylist = removeVideosFromPlaylist,
                resetRemoveVideosEvent = resetRemoveVideosEvent,
                onMenuClick = onMenuClick,
                onSortNodes = onSortNodes,
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

    @Test
    fun `test that VideoPlaylistBottomSheet is displayed when More action is clicked`() {
        setComposeContent()

        NodeSelectionAction.More.testTag.apply {
            assertIsDisplayedWithTag()
            getNodeWithTag().performClick()
        }

        VIDEO_PLAYLIST_DETAIL_BOTTOM_SHEET_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is displayed when showUpdateVideoPlaylistDialog is true`() {
        setComposeContent(
            videoPlaylistEditState = VideoPlaylistEditState(
                showUpdateVideoPlaylist = true
            )
        )

        VIDEO_PLAYLIST_DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is not displayed when showUpdateVideoPlaylistDialog is false`() {
        setComposeContent(
            videoPlaylistEditState = VideoPlaylistEditState()
        )

        VIDEO_PLAYLIST_DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that app bar shows selection count when videoSelectedCount is greater than zero`() {
        val selectedNode = mock<TypedNode> {
            on { id }.thenReturn(NodeId(1L))
        }
        val videos = listOf(
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("Video 1")
                on { duration }.thenReturn(10.seconds)
                on { durationString }.thenReturn("00:10")
            },
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(2L))
                on { name }.thenReturn("Video 2")
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
                playlistDetail = playlistDetail,
                selectedTypedNodes = setOf(selectedNode),
            )
        )

        composeTestRule.onNodeWithTag(
            VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("1", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that SelectAll action is displayed and invokes selectAll when clicked in selection mode`() {
        val selectedNode = mock<TypedNode> {
            on { id }.thenReturn(NodeId(1L))
        }
        val videos = listOf(
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("Video 1")
                on { duration }.thenReturn(10.seconds)
                on { durationString }.thenReturn("00:10")
            },
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(2L))
                on { name }.thenReturn("Video 2")
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
        var selectAllInvoked = false
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Data(
                playlistDetail = playlistDetail,
                selectedTypedNodes = setOf(selectedNode),
            ),
            selectAll = { selectAllInvoked = true }
        )

        NodeSelectionAction.SelectAll.testTag.assertIsDisplayedWithTag()
        composeTestRule.onNodeWithTag(NodeSelectionAction.SelectAll.testTag, useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        assertThat(selectAllInvoked).isTrue()
    }

    @Test
    fun `test that app bar shows playlist title when videoSelectedCount is zero`() {
        val playlistTitle = "My Playlist"
        val videos = listOf(
            mock<VideoUiEntity> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("Video 1")
                on { duration }.thenReturn(10.seconds)
                on { durationString }.thenReturn("00:10")
            }
        )
        val videoPlaylistEntity = mock<VideoPlaylistUiEntity> {
            on { thumbnailList }.thenReturn(emptyList())
            on { title }.thenReturn(playlistTitle)
        }
        val playlistDetail = mock<VideoPlaylistDetailUiEntity> {
            on { uiEntity }.thenReturn(videoPlaylistEntity)
            on { this.videos }.thenReturn(videos)
        }
        setComposeContent(
            uiState = VideoPlaylistDetailUiState.Data(
                playlistDetail = playlistDetail,
                selectedTypedNodes = emptySet(),
            )
        )

        VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        composeTestRule.onAllNodesWithText(playlistTitle, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
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

    private fun String.getNodeWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)
}