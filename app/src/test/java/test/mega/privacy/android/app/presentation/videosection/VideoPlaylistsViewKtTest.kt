package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAB_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PROGRESS_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlaylistsViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private fun setComposeContent(
        items: List<VideoPlaylistUIEntity> = emptyList(),
        progressBarShowing: Boolean = false,
        searchMode: Boolean = false,
        scrollToTop: Boolean = false,
        lazyListState: LazyListState = LazyListState(),
        sortOrder: String = "",
        isInputTitleValid: Boolean = true,
        shouldCreateVideoPlaylistDialog: Boolean = false,
        shouldDeleteVideoPlaylistDialog: Boolean = false,
        shouldRenameVideoPlaylistDialog: Boolean = false,
        inputPlaceHolderText: String = "",
        modifier: Modifier = Modifier,
        setShouldCreateVideoPlaylist: (Boolean) -> Unit = {},
        setShouldDeleteVideoPlaylist: (Boolean) -> Unit = {},
        setShouldRenameVideoPlaylist: (Boolean) -> Unit = {},
        setDialogInputPlaceholder: (String) -> Unit = {},
        setInputValidity: (Boolean) -> Unit = {},
        onClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit = { _, _ -> },
        onCreateDialogPositiveButtonClicked: (String) -> Unit = {},
        onRenameDialogPositiveButtonClicked: (playlistID: NodeId, newTitle: String) -> Unit = { _, _ -> },
        onDeleteDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = {},
        onDeletedMessageShown: () -> Unit = {},
        deletedVideoPlaylistTitles: List<String> = emptyList(),
        onSortOrderClick: () -> Unit = {},
        errorMessage: Int? = null,
        onLongClick: ((item: VideoPlaylistUIEntity, index: Int) -> Unit) = { _, _ -> },
    ) {
        composeTestRule.setContent {
            VideoPlaylistsView(
                items = items,
                progressBarShowing = progressBarShowing,
                searchMode = searchMode,
                scrollToTop = scrollToTop,
                lazyListState = lazyListState,
                sortOrder = sortOrder,
                isInputTitleValid = isInputTitleValid,
                shouldCreateVideoPlaylistDialog = shouldCreateVideoPlaylistDialog,
                shouldDeleteVideoPlaylistDialog = shouldDeleteVideoPlaylistDialog,
                shouldRenameVideoPlaylistDialog = shouldRenameVideoPlaylistDialog,
                inputPlaceHolderText = inputPlaceHolderText,
                modifier = modifier,
                setShouldCreateVideoPlaylist = setShouldCreateVideoPlaylist,
                setShouldDeleteVideoPlaylist = setShouldDeleteVideoPlaylist,
                setShouldRenameVideoPlaylist = setShouldRenameVideoPlaylist,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                onCreateDialogPositiveButtonClicked = onCreateDialogPositiveButtonClicked,
                setInputValidity = setInputValidity,
                onClick = onClick,
                onSortOrderClick = onSortOrderClick,
                errorMessage = errorMessage,
                onLongClick = onLongClick,
                onDeletedMessageShown = onDeletedMessageShown,
                deletedVideoPlaylistTitles = deletedVideoPlaylistTitles,
                onDeleteDialogPositiveButtonClicked = onDeleteDialogPositiveButtonClicked,
                onRenameDialogPositiveButtonClicked = onRenameDialogPositiveButtonClicked
            )
        }
    }

    @Test
    fun `test that CreateVideoPlaylistDialog is displayed correctly when shouldCreateVideoPlaylistDialog is true`() {
        setComposeContent(
            shouldCreateVideoPlaylistDialog = true
        )

        composeTestRule.onNodeWithTag(CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that CreateVideoPlaylistDialog is not displayed correctly when shouldCreateVideoPlaylistDialog is false`() {
        setComposeContent(
            shouldCreateVideoPlaylistDialog = false
        )

        composeTestRule.onNodeWithTag(CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that progressBar is displayed correctly`() {
        setComposeContent(
            progressBarShowing = true
        )

        composeTestRule.onNodeWithTag(PROGRESS_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed correctly`() {
        setComposeContent(
            items = emptyList()
        )

        composeTestRule.onNodeWithTag(VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that CreateVideoPlaylistFabButton calls the correct function`() {
        val setShowCreateVideoPlaylist = mock<(Boolean) -> Unit>()

        setComposeContent(
            setShouldCreateVideoPlaylist = setShowCreateVideoPlaylist,
        )

        composeTestRule.onNodeWithTag(FAB_BUTTON_TEST_TAG).performClick()

        verify(setShowCreateVideoPlaylist).invoke(true)
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is displayed correctly when shouldRenameVideoPlaylistDialog is true`() {
        setComposeContent(
            shouldRenameVideoPlaylistDialog = true
        )

        composeTestRule.onNodeWithTag(RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is not displayed correctly when shouldRenameVideoPlaylistDialog is false`() {
        setComposeContent(
            shouldRenameVideoPlaylistDialog = false
        )

        composeTestRule.onNodeWithTag(RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is displayed correctly when shouldDeleteVideoPlaylistDialog is true`() {
        setComposeContent(
            shouldDeleteVideoPlaylistDialog = true
        )

        composeTestRule.onNodeWithTag(DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is not displayed correctly when shouldDeleteVideoPlaylistDialog is false`() {
        setComposeContent(
            shouldDeleteVideoPlaylistDialog = false
        )

        composeTestRule.onNodeWithTag(DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsNotDisplayed()
    }
}