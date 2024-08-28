package mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_COPY_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_GET_LINK_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_HIDE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_MOVE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_REMOVE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_RENAME_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_RUBBISH_BIN_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SHARE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.VideoSectionTopBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class VideoSectionTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        tab: VideoSectionTab = VideoSectionTab.All,
        title: String = "",
        isActionMode: Boolean = false,
        selectedSize: Int = 0,
        searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
        query: String? = null,
        onMenuActionClick: (VideoSectionMenuAction?) -> Unit = {},
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        isHideMenuActionVisible: Boolean = false,
        isUnhideMenuActionVisible: Boolean = false,
        isRemoveLinkMenuActionVisible: Boolean = false,
        isRecentlyWatchedEnabled: Boolean = false
    ) {
        composeTestRule.setContent {
            VideoSectionTopBar(
                tab = tab,
                title = title,
                isActionMode = isActionMode,
                selectedSize = selectedSize,
                searchState = searchState,
                query = query,
                onMenuActionClicked = onMenuActionClick,
                onSearchTextChanged = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed,
                isHideMenuActionVisible = isHideMenuActionVisible,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                isRemoveLinkMenuActionVisible = isRemoveLinkMenuActionVisible,
                isRecentlyWatchedEnabled = isRecentlyWatchedEnabled
            )
        }
    }

    private val onMenuActionClick = mock<(VideoSectionMenuAction?) -> Unit>()

    @Test
    fun `test that the search top bar is displayed`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the select mode top bar is displayed`() {
        setComposeContent(isActionMode = true)

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that the hide action is not displayed when isHideMenuActionVisible is false`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
        )
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_HIDE_ACTION,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the remove link action is not displayed when isRemoveLinkMenuActionVisible is false`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
        )
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the remove link action is not displayed when selectSize more than 1`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 2,
            isRemoveLinkMenuActionVisible = true
        )
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the rename action is not displayed when selectSize more than 1`() {
        setComposeContent(isActionMode = true, selectedSize = 2)
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_RENAME_ACTION,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the unhide action is not displayed when isUnhideMenuActionVisible is false`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
        )
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that download action is pressed`() {
        initMenuAction(TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionDownloadAction)
    }

    private fun initMenuAction(testTag: String) {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            onMenuActionClick = onMenuActionClick
        )
        composeTestRule.onNodeWithTag(
            testTag = testTag,
            useUnmergedTree = true
        ).performClick()
    }

    @Test
    fun `test that get link action is pressed`() {
        initMenuAction(TEST_TAG_VIDEO_SECTION_GET_LINK_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionGetLinkAction)
    }

    @Test
    fun `test that send to chat action is pressed`() {
        initMenuAction(TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionSendToChatAction)
    }

    @Test
    fun `test that share action is pressed`() {
        initMenuAction(TEST_TAG_VIDEO_SECTION_SHARE_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionShareAction)
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
            onMenuActionClick = onMenuActionClick,
            isHideMenuActionVisible = true,
            isUnhideMenuActionVisible = true,
            isRemoveLinkMenuActionVisible = true
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
    fun `test that hide action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_HIDE_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionHideAction)
    }

    @Test
    fun `test that unhide action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionUnhideAction)
    }

    @Test
    fun `test that remove link action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionRemoveLinkAction)
    }

    @Test
    fun `test that rename action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_RENAME_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionRenameAction)
    }

    @Test
    fun `test that move action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_MOVE_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionMoveAction)
    }

    @Test
    fun `test that copy action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_COPY_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionCopyAction)
    }

    @Test
    fun `test that move to rubbish bin action is pressed`() {
        initMenuActionWithoutIcon(TEST_TAG_VIDEO_SECTION_RUBBISH_BIN_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionRubbishBinAction)
    }

    @Test
    fun `test that select all action is pressed when tab is Playlists`() {
        initMenuActionWithoutIconWhenTabIsPlaylist(TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionSelectAllAction)
    }

    private fun initMenuActionWithoutIconWhenTabIsPlaylist(testTag: String) {
        setComposeContent(
            tab = VideoSectionTab.Playlists,
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
    fun `test that clear selection action is pressed when tab is Playlists`() {
        initMenuActionWithoutIconWhenTabIsPlaylist(TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION)
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoSectionClearSelectionAction)
    }

    @Test
    fun `test that remove action is pressed when tab is Playlists`() {
        setComposeContent(
            tab = VideoSectionTab.Playlists,
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

    @Test
    fun `test that recently watched action is pressed`() {
        setComposeContent(
            onMenuActionClick = onMenuActionClick,
            isRecentlyWatchedEnabled = true
        )
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_ACTION,
            useUnmergedTree = true
        ).performClick()
        onMenuActionClick.invoke(VideoSectionMenuAction.VideoRecentlyWatchedAction)
    }
}

