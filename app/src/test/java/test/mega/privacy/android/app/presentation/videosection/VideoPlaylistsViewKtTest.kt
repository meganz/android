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
import mega.privacy.android.app.presentation.videosection.view.playlist.FAB_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PROGRESS_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
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
        showCreateAlbumDialog: Boolean = false,
        inputPlaceHolderText: String = "",
        modifier: Modifier = Modifier,
        setShowCreateVideoPlaylistDialog: (Boolean) -> Unit = {},
        setDialogInputPlaceholder: (String) -> Unit = {},
        onDialogPositiveButtonClicked: (title: String) -> Unit = {},
        setInputValidity: (Boolean) -> Unit = {},
        onClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit = { _, _ -> },
        onMenuClick: (VideoPlaylistUIEntity) -> Unit = { _ -> },
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
                showCreateVideoPlaylistDialog = showCreateAlbumDialog,
                inputPlaceHolderText = inputPlaceHolderText,
                modifier = modifier,
                setShowCreateVideoPlaylistDialog = setShowCreateVideoPlaylistDialog,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                setInputValidity = setInputValidity,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onSortOrderClick = onSortOrderClick,
                errorMessage = errorMessage,
                onLongClick = onLongClick
            )
        }
    }

    @Test
    fun `test that CreateVideoPlaylistDialog is displayed correctly when showCreateAlbumDialog is true`() {
        setComposeContent(
            showCreateAlbumDialog = true
        )

        composeTestRule.onNodeWithTag(CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that CreateVideoPlaylistDialog is not displayed correctly when showCreateAlbumDialog is false`() {
        setComposeContent(
            showCreateAlbumDialog = false
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
        val setShowCreateVideoPlaylistDialog = mock<(Boolean) -> Unit>()

        setComposeContent(
            setShowCreateVideoPlaylistDialog = setShowCreateVideoPlaylistDialog,
        )

        composeTestRule.onNodeWithTag(FAB_BUTTON_TEST_TAG).performClick()

        verify(setShowCreateVideoPlaylistDialog).invoke(true)
    }
}