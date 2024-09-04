package mega.privacy.android.app.presentation.videosection

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_LOADING_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_ITEM_CHECK_BOX_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_LIST_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videotoplaylist.VideoToPlaylistView
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoToPlaylistViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun setComposeContent(
        items: List<VideoPlaylistSetUiEntity> = emptyList(),
        isLoading: Boolean = false,
        isInputTitleValid: Boolean = true,
        showCreateVideoPlaylistDialog: Boolean = false,
        inputPlaceHolderText: String = "",
        setShouldCreateVideoPlaylist: (Boolean) -> Unit = {},
        onCreateDialogPositiveButtonClicked: (String) -> Unit = {},
        setInputValidity: (Boolean) -> Unit = {},
        setDialogInputPlaceholder: (String) -> Unit = {},
        searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
        query: String? = null,
        hasSelectedItems: Boolean = false,
        modifier: Modifier = Modifier,
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        onItemClicked: (Int, VideoPlaylistSetUiEntity) -> Unit = { _, _ -> },
        onDoneButtonClicked: () -> Unit = {},
        errorMessage: Int? = null,
    ) {
        composeTestRule.setContent {
            VideoToPlaylistView(
                items = items,
                searchState = searchState,
                query = query,
                hasSelectedItems = hasSelectedItems,
                modifier = modifier,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed,
                onItemClicked = onItemClicked,
                onDoneButtonClicked = onDoneButtonClicked,
                isLoading = isLoading,
                isInputTitleValid = isInputTitleValid,
                showCreateVideoPlaylistDialog = showCreateVideoPlaylistDialog,
                inputPlaceHolderText = inputPlaceHolderText,
                setShouldCreateVideoPlaylist = setShouldCreateVideoPlaylist,
                onCreateDialogPositiveButtonClicked = onCreateDialogPositiveButtonClicked,
                setInputValidity = setInputValidity,
                setDialogInputPlaceholder = setDialogInputPlaceholder,
                errorMessage = errorMessage
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when progressBarShowing is true`() {
        setComposeContent(isLoading = true)

        listOf(
            VIDEO_SECTION_LOADING_VIEW_TEST_TAG,
            VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG,
            VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG,
        ).map {
            it.isDisplayed()
        }
        VIDEO_TO_PLAYLIST_LIST_TEST_TAG.doesNotExist()
        VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG.doesNotExist()
    }

    @Test
    fun `test that the UIs are displayed correctly when the items are empty`() {
        setComposeContent()

        listOf(
            VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG,
            VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG,
            VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG,
        ).map {
            it.isDisplayed()
        }
        listOf(
            VIDEO_TO_PLAYLIST_LIST_TEST_TAG,
            VIDEO_SECTION_LOADING_VIEW_TEST_TAG
        ).map {
            it.doesNotExist()
        }
    }

    private fun String.isDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.doesNotExist() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun `test that the UIs are displayed correctly when the items are not empty`() {
        setComposeContent(initTestItems())

        listOf(
            VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG,
            VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG,
            VIDEO_TO_PLAYLIST_LIST_TEST_TAG,
        ).map {
            it.isDisplayed()
        }
        (0..3).map {
            VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG + it
        }.map {
            it.isDisplayed()
        }
        (0..3).map {
            VIDEO_TO_PLAYLIST_ITEM_CHECK_BOX_TEST_TAG + it
        }.map {
            it.isDisplayed()
        }
        listOf(
            VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG,
            VIDEO_SECTION_LOADING_VIEW_TEST_TAG
        ).map {
            it.doesNotExist()
        }
    }

    private fun initTestItems() = (0..3).map {
        VideoPlaylistSetUiEntity(
            id = it.toLong(),
            title = "Video Playlist Set $it"
        )
    }

    @Test
    fun `test that onDoneButtonClicked is invoked as expected`() {
        val onDoneButtonClicked: () -> Unit = mock()
        setComposeContent(
            items = initTestItems(),
            hasSelectedItems = true,
            onDoneButtonClicked = onDoneButtonClicked
        )

        VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG.performClick()
        verify(onDoneButtonClicked).invoke()
    }

    private fun String.performClick() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).performClick()

    @Test
    fun `test that expected functions are invoked when new playlist is clicked`() {
        val placeholderText = "New playlist"
        val setShouldCreateVideoPlaylist: (Boolean) -> Unit = mock()
        val setDialogInputPlaceholder: (String) -> Unit = mock()
        setComposeContent(
            setShouldCreateVideoPlaylist = setShouldCreateVideoPlaylist,
            setDialogInputPlaceholder = setDialogInputPlaceholder
        )

        VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG.performClick()
        verify(setShouldCreateVideoPlaylist).invoke(true)
        verify(setDialogInputPlaceholder).invoke(placeholderText)
    }

    @Test
    fun `test that onItemClicked is invoked as expected`() {
        val onItemClicked: (Int, VideoPlaylistSetUiEntity) -> Unit = mock()
        val testTitle = "Video Playlist Set"
        val testSet = VideoPlaylistSetUiEntity(
            id = 1L,
            title = testTitle
        )
        setComposeContent(items = listOf(testSet), onItemClicked = onItemClicked)

        (VIDEO_TO_PLAYLIST_ITEM_CHECK_BOX_TEST_TAG + 0).performClick()
        verify(onItemClicked).invoke(0, testSet)
    }

    @Test
    fun `test that create video playlist dialog is displayed as expected`() {
        setComposeContent(showCreateVideoPlaylistDialog = true)

        VIDEO_TO_PLAYLIST_CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.isDisplayed()
    }

    @Test
    fun `test that onDialogPositiveButtonClicked is invoked as expected`() {
        val onDialogPositiveButtonClicked: (String) -> Unit = mock()
        setComposeContent(
            showCreateVideoPlaylistDialog = true,
            onCreateDialogPositiveButtonClicked = onDialogPositiveButtonClicked
        )


        VIDEO_TO_PLAYLIST_CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG.isDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.general_create)).performClick()
        verify(onDialogPositiveButtonClicked).invoke(anyOrNull())
    }
}