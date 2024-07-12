package test.mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_MORE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_REMOVE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailTopBar
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        title: String = "",
        isActionMode: Boolean = false,
        selectedSize: Int = 0,
        onMenuActionClick: (VideoSectionMenuAction?) -> Unit = {},
        onBackPressed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailTopBar(
                title = title,
                isActionMode = isActionMode,
                selectedSize = selectedSize,
                onMenuActionClick = onMenuActionClick,
                onBackPressed = onBackPressed
            )
        }
    }

    private val onMenuActionClick = mock<(VideoSectionMenuAction?) -> Unit>()

    @Test
    fun `test that the top bar is displayed`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the select mode top bar is displayed`() {
        setComposeContent(isActionMode = true)

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that more action is pressed`() {
        setComposeContent(onMenuActionClick = onMenuActionClick)
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_MORE_ACTION,
            useUnmergedTree = true
        ).performClick()
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionMoreAction)
    }

    @Test
    fun `test that select all action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionSelectAllAction)
    }

    private fun initMenuActionWithoutIcon(testTag: String) {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            onMenuActionClick = onMenuActionClick
        )
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = testTag,
            useUnmergedTree = true
        ).apply {
            assertIsDisplayed()
            performClick()
        }
    }

    @Test
    fun `test that clear selection action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionClearSelectionAction)
    }

    @Test
    fun `test that remove action is pressed when tab is Playlists`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            onMenuActionClick = onMenuActionClick
        )
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_REMOVE_ACTION,
            useUnmergedTree = true
        ).performClick()
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionRemoveAction)
    }
}