package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerSubtitleUiState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SelectSubtitleViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val subtitleFileInfo = SubtitleFileInfo(
        id = 1L,
        name = "subtitle.srt",
        url = null,
        parentName = "Movies",
        isMarkedSensitive = false,
        isSensitiveInherited = false,
    )

    private fun setComposeContent(
        uiState: VideoPlayerSubtitleUiState = VideoPlayerSubtitleUiState(isLoading = true),
        onSearchTextChange: (String) -> Unit = {},
        itemClicked: (SubtitleFileInfo) -> Unit = {},
        onAddSubtitle: (SubtitleFileInfo?) -> Unit = {},
        onBackPressed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SelectSubtitleView(
                uiState = uiState,
                onSearchTextChange = onSearchTextChange,
                itemClicked = itemClicked,
                onAddSubtitle = onAddSubtitle,
                onBackPressed = onBackPressed,
            )
        }
    }

    @Test
    fun `test that progress indicator is shown when state is loading`() {
        setComposeContent(uiState = VideoPlayerSubtitleUiState(isLoading = true))

        VIDEO_PLAYER_SELECT_SUBTITLE_PROGRESS_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that file list is not shown when state is loading`() {
        setComposeContent(uiState = VideoPlayerSubtitleUiState(isLoading = true))

        VIDEO_PLAYER_SELECT_SUBTITLE_FILES_TEST_TAG.assertDoesNotExist()
    }

    @Test
    fun `test that file list is shown when state is success with items`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
            )
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_FILES_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that empty view is shown when items are empty and query is empty`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = emptyList(),
                query = "",
            ),
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_EMPTY_LIST_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that search empty view is shown when items are empty and query is not empty`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = emptyList(),
                query = "no match",
            ),
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_EMPTY_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that add subtitle button is disabled when no subtitle is selected`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
                selectedSubtitleFileInfo = null,
            ),
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_ADD_BUTTON_TEST_TAG.assertIsNotEnabled()
    }

    @Test
    fun `test that add subtitle button is enabled when a subtitle is selected`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
                selectedSubtitleFileInfo = subtitleFileInfo,
            ),
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_ADD_BUTTON_TEST_TAG.assertIsEnabled()
    }

    @Test
    fun `test that onAddSubtitle is invoked when add button is clicked`() {
        val onAddSubtitle = mock<(SubtitleFileInfo?) -> Unit>()
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
                selectedSubtitleFileInfo = subtitleFileInfo,
            ),
            onAddSubtitle = onAddSubtitle,
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_ADD_BUTTON_TEST_TAG.performClick()
        verify(onAddSubtitle).invoke(subtitleFileInfo)
    }

    @Test
    fun `test that onBackPressed is invoked when cancel button is clicked`() {
        val onBackPressed = mock<() -> Unit>()
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
            ),
            onBackPressed = onBackPressed,
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_CANCEL_BUTTON_TEST_TAG.performClick()
        verify(onBackPressed).invoke()
    }

    @Test
    fun `test that search bar is displayed when items are not empty`() {
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
            )
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_INPUT_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that search bar is not displayed when state is loading`() {
        setComposeContent(uiState = VideoPlayerSubtitleUiState(isLoading = true))

        VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_INPUT_TEST_TAG.assertDoesNotExist()
    }

    @Test
    fun `test that onSearchTextChange is invoked when text is entered in search bar`() {
        val onSearchTextChange = mock<(String) -> Unit>()
        setComposeContent(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
            ),
            onSearchTextChange = onSearchTextChange,
        )

        VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_INPUT_TEST_TAG.performTextInput("movie")
        verify(onSearchTextChange).invoke("movie")
    }

    private fun String.assertIsDisplayed() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun String.assertDoesNotExist() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun String.assertIsEnabled() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsEnabled()
    }

    private fun String.assertIsNotEnabled() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsNotEnabled()
    }

    private fun String.performClick() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).performClick()
    }

    private fun String.performTextInput(text: String) {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).performTextInput(text)
    }
}
