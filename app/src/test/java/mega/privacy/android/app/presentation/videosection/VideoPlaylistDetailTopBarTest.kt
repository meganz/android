package mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_HIDE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_MORE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_REMOVE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SHARE_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_SORT_BY_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailTopBar
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlaylistDetailTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        title: String = "",
        isSystemVideoPlaylist: Boolean = false,
        isActionMode: Boolean = false,
        selectedSize: Int = 0,
        onMenuActionClick: (VideoSectionMenuAction?) -> Unit = {},
        onBackPressed: () -> Unit = {},
        enableFavouritesPlaylistMenu: Boolean = false,
        isHideMenuActionVisible: Boolean = false,
        isUnhideMenuActionVisible: Boolean = false,
    ) {
        composeTestRule.setContent {
            VideoPlaylistDetailTopBar(
                title = title,
                isActionMode = isActionMode,
                selectedSize = selectedSize,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                isHideMenuActionVisible = isHideMenuActionVisible,
                onMenuActionClick = onMenuActionClick,
                onBackPressed = onBackPressed,
                isSystemVideoPlaylist = isSystemVideoPlaylist,
                enableFavouritesPlaylistMenu = enableFavouritesPlaylistMenu
            )
        }
    }

    private val onMenuActionClick = mock<(VideoSectionMenuAction?) -> Unit>()

    @Test
    fun `test that the top bar is displayed`() {
        setComposeContent()

        VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG.assertIsDisplayed()
        VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG.assertIsNotDisplayed()
    }

    private fun String.assertIsDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()


    @Test
    fun `test that the select mode top bar is displayed`() {
        setComposeContent(isActionMode = true)

        VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG.assertIsNotDisplayed()
        VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that more action is pressed`() {
        setComposeContent(onMenuActionClick = onMenuActionClick)
        TEST_TAG_VIDEO_SECTION_MORE_ACTION.performClick()
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionMoreAction)
    }

    private fun String.performClick() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).performClick()

    @Test
    fun `test that select all action is pressed`() {
        initMenuActionUnderSelectionMode(TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION)
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionSelectAllAction)
    }

    private fun initMenuActionUnderSelectionMode(
        testTag: String,
        isWithoutIcon: Boolean = true,
        isSystemVideoPlaylist: Boolean = false,
        isHideMenuActionVisible: Boolean = false,
        isUnhideMenuActionVisible: Boolean = false,
    ) {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            isSystemVideoPlaylist = isSystemVideoPlaylist,
            isHideMenuActionVisible = isHideMenuActionVisible,
            isUnhideMenuActionVisible = isUnhideMenuActionVisible,
            onMenuActionClick = onMenuActionClick
        )
        if (isWithoutIcon) {
            TAG_MENU_ACTIONS_SHOW_MORE.apply {
                assertIsDisplayed()
                performClick()
            }
        }
        testTag.apply {
            assertIsDisplayed()
            performClick()
        }
    }

    @Test
    fun `test that clear selection action is pressed`() {
        initMenuActionUnderSelectionMode(TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION)
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionClearSelectionAction)
    }

    @Test
    fun `test that remove action is pressed when tab is Playlists`() {
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            onMenuActionClick = onMenuActionClick
        )
        TEST_TAG_VIDEO_SECTION_REMOVE_ACTION.performClick()
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionRemoveAction)
    }

    @Test
    fun `test that more action is not displayed when isSystemVideoPlaylist is true`() {
        setComposeContent(isSystemVideoPlaylist = true)
        TEST_TAG_VIDEO_SECTION_MORE_ACTION.assertIsNotDisplayed()
    }

    @Test
    fun `test that more action is displayed when isSystemVideoPlaylist is false`() {
        setComposeContent(isSystemVideoPlaylist = false, enableFavouritesPlaylistMenu = true)
        TEST_TAG_VIDEO_SECTION_MORE_ACTION.assertIsDisplayed()
    }

    @Test
    fun `test that sort by action is displayed and VideoSectionSortByAction is invoked after is pressed`() {
        setComposeContent(
            isSystemVideoPlaylist = true,
            enableFavouritesPlaylistMenu = true,
            onMenuActionClick = onMenuActionClick
        )
        TEST_TAG_VIDEO_SECTION_SORT_BY_ACTION.apply {
            assertIsDisplayed()
            performClick()
        }
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionSortByAction)
    }

    @Test
    fun `test that more action is displayed under selection mode`() {
        setComposeContent(isActionMode = true, selectedSize = 1, isSystemVideoPlaylist = true)
        TEST_TAG_VIDEO_SECTION_MORE_ACTION.assertIsDisplayed()
    }

    @Test
    fun `test that download action is displayed and VideoSectionDownloadAction is invoked`() {
        initMenuActionUnderSelectionMode(
            testTag = TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION,
            isWithoutIcon = false,
            isSystemVideoPlaylist = true
        )
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionDownloadAction)
    }

    @Test
    fun `test that send to chat action is displayed and VideoSectionSendToChatAction is invoked`() {
        initMenuActionUnderSelectionMode(
            testTag = TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION,
            isWithoutIcon = false,
            isSystemVideoPlaylist = true
        )
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionSendToChatAction)
    }

    @Test
    fun `test that share action is displayed and VideoSectionShareAction is invoked`() {
        initMenuActionUnderSelectionMode(
            testTag = TEST_TAG_VIDEO_SECTION_SHARE_ACTION,
            isWithoutIcon = false,
            isSystemVideoPlaylist = true
        )
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionShareAction)
    }

    @Test
    fun `test that hide action is displayed and VideoSectionHideAction is invoked`() {
        initMenuActionUnderSelectionMode(
            testTag = TEST_TAG_VIDEO_SECTION_HIDE_ACTION,
            isHideMenuActionVisible = true,
        )
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionHideAction)
    }

    @Test
    fun `test that unhide action is displayed and VideoSectionUnhideAction is invoked`() {
        initMenuActionUnderSelectionMode(
            testTag = TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION,
            isUnhideMenuActionVisible = true,
        )
        verify(onMenuActionClick).invoke(VideoSectionMenuAction.VideoSectionUnhideAction)
    }
}