package test.mega.privacy.android.app.presentation.videosection

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_MORE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_REMOVE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_PLAY_ALL_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.PLAYLIST_TOTAL_DURATION_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoilApi::class)
@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private val playlist = mock<VideoPlaylistUIEntity> {
        on { id }.thenReturn(NodeId(1L))
        on { title }.thenReturn("title")
    }

    private fun setComposeContent(
        playlist: VideoPlaylistUIEntity? = null,
        selectedSize: Int = 0,
        modifier: Modifier = Modifier,
        isInputTitleValid: Boolean = true,
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
        onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = {},
        removedMessageShown: () -> Unit = {},
        numberOfRemovedItems: Int = 0,
        onPlayAllClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        onMenuActionClick: (VideoSectionMenuAction?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailView(
                playlist = playlist,
                selectedSize = selectedSize,
                modifier = modifier,
                isInputTitleValid = isInputTitleValid,
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
                onDeleteVideosDialogPositiveButtonClicked = onDeleteVideosDialogPositiveButtonClicked,
                removedMessageShown = removedMessageShown,
                numberOfRemovedItems = numberOfRemovedItems,
                onPlayAllClicked = onPlayAllClicked,
                onBackPressed = onBackPressed,
                onMenuActionClick = onMenuActionClick
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when the playlist is null`() {
        setComposeContent(null)

        VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG.assertIsDisplayed()
        PLAYLIST_TITLE_TEST_TAG.assertIsDisplayed()
        PLAYLIST_TITLE_TEST_TAG.assertTextEquals("")
        PLAYLIST_TOTAL_DURATION_TEST_TAG.assertIsDisplayed()
        PLAYLIST_TOTAL_DURATION_TEST_TAG.assertTextEquals("00:00:00")
        PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG.assertIsDisplayed()
        PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG.assertTextEquals("no videos")
    }

    private fun String.assertIsDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun String.assertTextEquals(value: String) =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)
            .assertTextEquals(value)

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

        PLAYLIST_TITLE_TEST_TAG.assertIsDisplayed()
        PLAYLIST_TITLE_TEST_TAG.assertTextEquals(expectedTitle)
        PLAYLIST_TOTAL_DURATION_TEST_TAG.assertIsDisplayed()
        PLAYLIST_TOTAL_DURATION_TEST_TAG.assertTextEquals(expectedTotalDuration)
        PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG.assertIsDisplayed()
        PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG.assertTextEquals("2 videos")
    }

    @Test
    fun `test that RenameVideoPlaylistDialog is displayed`() {
        setComposeContent(playlist = playlist)

        TEST_TAG_VIDEO_SECTION_MORE_ACTION.performClick()
        VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG.performClick()
        DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    private fun String.performClick() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).performClick()

    @Test
    fun `test that RenameVideoPlaylistDialog is not displayed by default`() {
        setComposeContent(playlist = playlist)

        DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is displayed`() {
        setComposeContent(playlist = playlist)

        TEST_TAG_VIDEO_SECTION_MORE_ACTION.performClick()
        VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG.performClick()
        DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideosDialog is not displayed by default`() {
        setComposeContent(playlist = playlist)

        DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    @Test
    fun `test that DeleteVideosDialog is displayed`() {
        setComposeContent(playlist = playlist, selectedSize = 1)

        TEST_TAG_VIDEO_SECTION_REMOVE_ACTION.performClick()
        DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that DeleteVideoPlaylistDialog is not displayed by default`() {
        setComposeContent(playlist = playlist)

        DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.assertIsNotDisplayed()
    }

    @Test
    fun `test that onPlayAllClicked is invoked when the play all button is clicked`() {
        val onPlayAllClicked = mock<() -> Unit>()
        val expectedTitle = "new playlist"
        val expectedTotalDuration = "10:00:00"
        val expectedNumberOfVideos = 1

        val testVideo = mock<VideoUIEntity> {
            on { id }.thenReturn(NodeId(123456L))
            on { name }.thenReturn("video")
            on { size }.thenReturn(700L)
            on { durationString }.thenReturn("10:00")
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

        DETAIL_PLAY_ALL_BUTTON_TEST_TAG.performClick()
        verify(onPlayAllClicked).invoke()
    }
}