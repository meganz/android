package mega.privacy.android.app.presentation.videoplayer

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_COPY_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_HIDE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_MOVE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_RENAME_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_SHARE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerAddToAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerChatImportAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerCopyAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerDownloadAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerFileInfoAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerGetLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerHideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerMoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRenameAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRubbishBinAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSendToChatAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerShareAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerUnhideAction
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerTopBar
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlayerTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onMenuActionClick = mock<(VideoPlayerMenuAction?) -> Unit>()

    private fun setComposeContent(
        title: String = "",
        menuActions: List<VideoPlayerMenuAction> = emptyList(),
        onBackPressed: () -> Unit = {},
        onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit = onMenuActionClick,
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlayerTopBar(
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
        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION to VideoPlayerDownloadAction,
        TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION to VideoPlayerSendToChatAction,
        TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION to VideoPlayerChatImportAction,
        TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION to VideoPlayerSaveForOfflineAction,
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
            TEST_TAG_VIDEO_PLAYER_SHARE_ACTION to VideoPlayerShareAction,
            TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION to VideoPlayerGetLinkAction,
            TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION to VideoPlayerRemoveLinkAction,
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
                TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION -> VideoPlayerDownloadAction
                TEST_TAG_VIDEO_PLAYER_SHARE_ACTION -> VideoPlayerShareAction
                TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION -> VideoPlayerSendToChatAction
                TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION -> VideoPlayerGetLinkAction
                TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION -> VideoPlayerRemoveLinkAction
                TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION -> VideoPlayerFileInfoAction
                TEST_TAG_VIDEO_PLAYER_RENAME_ACTION -> VideoPlayerRenameAction
                TEST_TAG_VIDEO_PLAYER_HIDE_ACTION -> VideoPlayerHideAction
                TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION -> VideoPlayerUnhideAction
                TEST_TAG_VIDEO_PLAYER_MOVE_ACTION -> VideoPlayerMoveAction
                TEST_TAG_VIDEO_PLAYER_COPY_ACTION -> VideoPlayerCopyAction
                TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION -> VideoPlayerAddToAction
                TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION -> VideoPlayerRubbishBinAction
                TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION -> VideoPlayerRemoveAction
                TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION -> VideoPlayerChatImportAction
                TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION -> VideoPlayerSaveForOfflineAction
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
        TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION to VideoPlayerFileInfoAction,
        TEST_TAG_VIDEO_PLAYER_RENAME_ACTION to VideoPlayerRenameAction,
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION to VideoPlayerHideAction,
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION to VideoPlayerUnhideAction,
        TEST_TAG_VIDEO_PLAYER_MOVE_ACTION to VideoPlayerMoveAction,
        TEST_TAG_VIDEO_PLAYER_COPY_ACTION to VideoPlayerCopyAction,
        TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION to VideoPlayerAddToAction,
        TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION to VideoPlayerRubbishBinAction,
        TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION to VideoPlayerRemoveAction
    )
}