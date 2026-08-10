package mega.privacy.android.app.mediaplayer.videoplayer.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class LegacyVideoPlayerTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onMenuActionClick = Mockito.mock<(VideoPlayerMenuAction?) -> Unit>()

    private fun setComposeContent(
        title: String = "",
        menuActions: List<VideoPlayerMenuAction> = emptyList(),
        onBackPressed: () -> Unit = {},
        onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit = onMenuActionClick,
        modifier: Modifier = Modifier.Companion,
    ) {
        composeTestRule.setContent {
            LegacyVideoPlayerTopBar(
                title = title,
                menuActions = menuActions,
                onBackPressed = onBackPressed,
                onMenuActionClicked = onMenuActionClicked,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that menu options with ShownAsAction set to always are correctly displayed`() {
        val menuOptions = getMenuOptionsAlwaysShowAsAction()
        setComposeContent(
            menuActions = menuOptions.map { it.second },
        )

        menuOptions.map { it.first }.onEach {
            it.isDisplayedAndCheckClicked()
        }
    }

    private fun getMenuOptionsAlwaysShowAsAction() = listOf<Pair<String, VideoPlayerMenuAction>>(
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION to VideoPlayerMenuAction.VideoPlayerDownloadAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION to VideoPlayerMenuAction.VideoPlayerSendToChatAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION to VideoPlayerMenuAction.VideoPlayerChatImportAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION to VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction,
    )

    @Test
    fun `test that share related menu options with ShownAsAction set to always are correctly displayed`() {
        val menuOptions = getMenuOptionsAlwaysShowAsActionRegardingShare()
        setComposeContent(
            menuActions = menuOptions.map { it.second },
        )

        menuOptions.map { it.first }.onEach {
            it.isDisplayedAndCheckClicked()
        }
    }

    private fun getMenuOptionsAlwaysShowAsActionRegardingShare() =
        listOf<Pair<String, VideoPlayerMenuAction>>(
            VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SHARE_ACTION to VideoPlayerMenuAction.VideoPlayerShareAction,
            VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION to VideoPlayerMenuAction.VideoPlayerGetLinkAction,
            VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION to VideoPlayerMenuAction.VideoPlayerRemoveLinkAction,
        )

    private fun String.isDisplayedAndCheckClicked(needMoreActionClicked: Boolean = false) {
        if (needMoreActionClicked) {
            moreActionClicked()
        }
        composeTestRule.onNodeWithTag(
            testTag = this, useUnmergedTree = true
        ).assertIsDisplayed()
        checkMenuActionClicked()
    }

    private fun String.performClick() = composeTestRule.onNodeWithTag(
        testTag = this, useUnmergedTree = true
    ).performClick()

    private fun String.checkMenuActionClicked() {
        performClick()
        verify(onMenuActionClick).invoke(
            when (this) {
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION -> VideoPlayerMenuAction.VideoPlayerDownloadAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SHARE_ACTION -> VideoPlayerMenuAction.VideoPlayerShareAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION -> VideoPlayerMenuAction.VideoPlayerSendToChatAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION -> VideoPlayerMenuAction.VideoPlayerGetLinkAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION -> VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION -> VideoPlayerMenuAction.VideoPlayerFileInfoAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_RENAME_ACTION -> VideoPlayerMenuAction.VideoPlayerRenameAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_HIDE_ACTION -> VideoPlayerMenuAction.VideoPlayerHideAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION -> VideoPlayerMenuAction.VideoPlayerUnhideAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_MOVE_ACTION -> VideoPlayerMenuAction.VideoPlayerMoveAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_COPY_ACTION -> VideoPlayerMenuAction.VideoPlayerCopyAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION -> VideoPlayerMenuAction.VideoPlayerAddToAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION -> VideoPlayerMenuAction.VideoPlayerRubbishBinAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION -> VideoPlayerMenuAction.VideoPlayerRemoveAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION -> VideoPlayerMenuAction.VideoPlayerChatImportAction
                VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION -> VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
                else -> null
            }
        )
    }

    private fun moreActionClicked() {
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
    }

    @Test
    fun `test that menu options with ShownAsAction set to never are correctly displayed`() {
        val menuOptions = getMenuOptionsNeverShowAsAction()
        setComposeContent(
            menuActions = menuOptions.map { it.second },
        )

        menuOptions.map { it.first }.onEach {
            it.isDisplayedAndCheckClicked(true)
        }
    }

    private fun getMenuOptionsNeverShowAsAction() = listOf<Pair<String, VideoPlayerMenuAction>>(
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION to VideoPlayerMenuAction.VideoPlayerFileInfoAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_RENAME_ACTION to VideoPlayerMenuAction.VideoPlayerRenameAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_HIDE_ACTION to VideoPlayerMenuAction.VideoPlayerHideAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION to VideoPlayerMenuAction.VideoPlayerUnhideAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_MOVE_ACTION to VideoPlayerMenuAction.VideoPlayerMoveAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_COPY_ACTION to VideoPlayerMenuAction.VideoPlayerCopyAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION to VideoPlayerMenuAction.VideoPlayerAddToAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION to VideoPlayerMenuAction.VideoPlayerRubbishBinAction,
        VideoPlayerMenuAction.TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION to VideoPlayerMenuAction.VideoPlayerRemoveAction
    )
}