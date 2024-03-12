package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_PLAY_ALL_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TOTAL_DURATION_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val playlist = mock<VideoPlaylistUIEntity> {
        on { id }.thenReturn(NodeId(1L))
        on { title }.thenReturn("title")
    }

    private fun setComposeContent(
        playlist: VideoPlaylistUIEntity? = null,
        modifier: Modifier = Modifier,
        isInputTitleValid: Boolean = true,
        shouldDeleteVideoPlaylistDialog: Boolean = false,
        shouldRenameVideoPlaylistDialog: Boolean = false,
        shouldShowVideoPlaylistBottomSheetDetails: Boolean = false,
        setShouldDeleteVideoPlaylistDialog: (Boolean) -> Unit = {},
        setShouldRenameVideoPlaylistDialog: (Boolean) -> Unit = {},
        setShouldShowVideoPlaylistBottomSheetDetails: (Boolean) -> Unit = {},
        inputPlaceHolderText: String = "",
        setInputValidity: (Boolean) -> Unit = {},
        onRenameDialogPositiveButtonClicked: (playlistID: NodeId, newTitle: String) -> Unit = { _, _ -> },
        onDeleteDialogPositiveButtonClicked: (List<VideoPlaylistUIEntity>) -> Unit = { _ -> },
        onAddElementsClicked: () -> Unit = {},
        errorMessage: Int? = null,
        onClick: (item: VideoUIEntity, index: Int) -> Unit = { _, _ -> },
        onMenuClick: (VideoUIEntity) -> Unit = { _ -> },
        onLongClick: ((item: VideoUIEntity, index: Int) -> Unit) = { _, _ -> },
        numberOfAddedVideos: Int = 0,
        addedMessageShown: () -> Unit = {},
        shouldDeleteVideosDialog: Boolean = false,
        setShouldDeleteVideosDialog: (Boolean) -> Unit = {},
        onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = {},
        removedMessageShown: () -> Unit = {},
        numberOfRemovedItems: Int = 0,
        onPlayAllClicked: () -> Unit = {},
        onUpdatedTitle: (String?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailView(
                playlist = playlist,
                modifier = modifier,
                isInputTitleValid = isInputTitleValid,
                shouldDeleteVideoPlaylistDialog = shouldDeleteVideoPlaylistDialog,
                shouldRenameVideoPlaylistDialog = shouldRenameVideoPlaylistDialog,
                shouldShowVideoPlaylistBottomSheetDetails = shouldShowVideoPlaylistBottomSheetDetails,
                setShouldDeleteVideoPlaylistDialog = setShouldDeleteVideoPlaylistDialog,
                setShouldRenameVideoPlaylistDialog = setShouldRenameVideoPlaylistDialog,
                setShouldShowVideoPlaylistBottomSheetDetails = setShouldShowVideoPlaylistBottomSheetDetails,
                inputPlaceHolderText = inputPlaceHolderText,
                setInputValidity = setInputValidity,
                onRenameDialogPositiveButtonClicked = onRenameDialogPositiveButtonClicked,
                onDeleteDialogPositiveButtonClicked = onDeleteDialogPositiveButtonClicked,
                onAddElementsClicked = onAddElementsClicked,
                errorMessage = errorMessage,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                numberOfAddedVideos = numberOfAddedVideos,
                addedMessageShown = addedMessageShown,
                shouldDeleteVideosDialog = shouldDeleteVideosDialog,
                setShouldDeleteVideosDialog = setShouldDeleteVideosDialog,
                onDeleteVideosDialogPositiveButtonClicked = onDeleteVideosDialogPositiveButtonClicked,
                removedMessageShown = removedMessageShown,
                numberOfRemovedItems = numberOfRemovedItems,
                onPlayAllClicked = onPlayAllClicked,
                onUpdatedTitle = onUpdatedTitle
            )
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

    @Test
    fun `test that RenameVideoPlaylistDialog is displayed correctly when shouldRenameVideoPlaylistDialog is true`() {
        setComposeContent(
            playlist = playlist,
            shouldRenameVideoPlaylistDialog = true
        )

        composeTestRule.onNodeWithTag(DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is not displayed correctly when shouldRenameVideoPlaylistDialog is false`() {
        setComposeContent(
            playlist = playlist,
            shouldRenameVideoPlaylistDialog = false
        )

        composeTestRule.onNodeWithTag(DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is displayed correctly when shouldDeleteVideoPlaylistDialog is true`() {
        setComposeContent(
            playlist = playlist,
            shouldDeleteVideoPlaylistDialog = true
        )

        composeTestRule.onNodeWithTag(DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideosDialog is not displayed correctly when shouldDeleteVideosDialog is false`() {
        setComposeContent(
            playlist = playlist,
            shouldDeleteVideosDialog = false
        )

        composeTestRule.onNodeWithTag(DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideosDialog is displayed correctly when shouldDeleteVideosDialog is true`() {
        setComposeContent(
            playlist = playlist,
            shouldDeleteVideosDialog = true
        )

        composeTestRule.onNodeWithTag(DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is not displayed correctly when shouldDeleteVideoPlaylistDialog is false`() {
        setComposeContent(
            playlist = playlist,
            shouldDeleteVideoPlaylistDialog = false
        )

        composeTestRule.onNodeWithTag(DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that onPlayAllClicked is invoked when the play all button is clicked`() {
        val onPlayAllClicked = mock<() -> Unit>()
        val expectedTitle = "new playlist"
        val expectedTotalDuration = "10:00:00"
        val expectedNumberOfVideos = 1

        val testVideo = mock<VideoUIEntity> {
            on { name }.thenReturn("video")
        }

        val playlist = VideoPlaylistUIEntity(
            id = NodeId(1L),
            title = expectedTitle,
            cover = null,
            creationTime = 0,
            modificationTime = 0,
            thumbnailList = null,
            numberOfVideos = expectedNumberOfVideos,
            totalDuration = expectedTotalDuration,
            videos = listOf(testVideo)
        )

        setComposeContent(
            playlist = playlist,
            onPlayAllClicked = onPlayAllClicked
        )

        composeTestRule.onNodeWithTag(DETAIL_PLAY_ALL_BUTTON_TEST_TAG).performClick()
        verify(onPlayAllClicked).invoke()
    }
}