package mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_LOADING_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAB_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlaylistsViewTest {
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
        showDeleteVideoPlaylistDialog: Boolean = false,
        inputPlaceHolderText: String = "",
        modifier: Modifier = Modifier,
        updateShowDeleteVideoPlaylist: (Boolean) -> Unit = {},
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
        onDeletePlaylistsDialogPositiveButtonClicked: () -> Unit = {},
        onDeleteDialogNegativeButtonClicked: () -> Unit = {}
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
                showDeleteVideoPlaylistDialog = showDeleteVideoPlaylistDialog,
                inputPlaceHolderText = inputPlaceHolderText,
                modifier = modifier,
                updateShowDeleteVideoPlaylist = updateShowDeleteVideoPlaylist,
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
                onRenameDialogPositiveButtonClicked = onRenameDialogPositiveButtonClicked,
                onDeletePlaylistsDialogPositiveButtonClicked = onDeletePlaylistsDialogPositiveButtonClicked,
                onDeleteDialogNegativeButtonClicked = onDeleteDialogNegativeButtonClicked
            )
        }
    }

    @Test
    fun `test that CreateVideoPlaylistDialog is displayed`() {
        setComposeContent()

        FAB_BUTTON_TEST_TAG.performClick()
        CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    private fun String.performClick() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).performClick()

    private fun String.assertIsDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    @Test
    fun `test that CreateVideoPlaylistDialog is not displayed by default`() {
        setComposeContent()

        CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    private fun String.assertIsNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    @Test
    fun `test that progressBar is displayed correctly`() {
        setComposeContent(progressBarShowing = true)

        VIDEO_SECTION_LOADING_VIEW_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed correctly`() {
        setComposeContent(items = emptyList())

        VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is displayed`() {
        val entity = mock<VideoPlaylistUIEntity> {
            on { title }.thenReturn("title")
        }
        setComposeContent(items = listOf(entity))

        composeTestRule.onNodeWithContentDescription(label = "3 dots", useUnmergedTree = true)
            .performClick()
        VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG.performClick()
        RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is not displayed by default`() {
        setComposeContent()

        RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is displayed correctly when shouldDeleteVideoPlaylistDialog is true`() {
        setComposeContent(showDeleteVideoPlaylistDialog = true)

        DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is not displayed correctly when shouldDeleteVideoPlaylistDialog is false`() {
        setComposeContent(showDeleteVideoPlaylistDialog = false)

        DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    @Test
    fun `test that setShouldDeleteVideoPlaylist function is invoked as expected`() {
        val setShouldDeleteVideoPlaylist = mock<(Boolean) -> Unit>()
        val entity = mock<VideoPlaylistUIEntity> {
            on { title }.thenReturn("title")
        }
        setComposeContent(
            items = listOf(entity),
            updateShowDeleteVideoPlaylist = setShouldDeleteVideoPlaylist
        )

        composeTestRule.onNodeWithContentDescription(label = "3 dots", useUnmergedTree = true)
            .performClick()
        VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG.performClick()

        verify(setShouldDeleteVideoPlaylist).invoke(true)
    }
}