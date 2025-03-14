package mega.privacy.android.app.presentation.videoplayer

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerTopBar
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
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
        launchSource: Int = INVALID_VALUE,
        isInRubbish: Boolean = false,
        canRemoveFromChat: Boolean = false,
        nodeIsNull: Boolean = false,
        shouldShowHideNode: Boolean = false,
        shouldShowUnhideNode: Boolean = false,
        shouldShowShare: Boolean = false,
        shouldShowGetLink: Boolean = false,
        shouldShowRemoveLink: Boolean = false,
        isAccess: Boolean = false,
        isRubbishBinShown: Boolean = false,
        shouldShowAddTo: Boolean = false,
        onBackPressed: () -> Unit = {},
        onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit = onMenuActionClick,
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlayerTopBar(
                title = title,
                launchSource = launchSource,
                isInRubbish = isInRubbish,
                canRemoveFromChat = canRemoveFromChat,
                nodeIsNull = nodeIsNull,
                shouldShowHideNode = shouldShowHideNode,
                shouldShowUnhideNode = shouldShowUnhideNode,
                shouldShowShare = shouldShowShare,
                shouldShowGetLink = shouldShowGetLink,
                shouldShowRemoveLink = shouldShowRemoveLink,
                isAccess = isAccess,
                isRubbishBinShown = isRubbishBinShown,
                shouldShowAddTo = shouldShowAddTo,
                onBackPressed = onBackPressed,
                onMenuActionClicked = onMenuActionClicked,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is OFFLINE_ADAPTER`() {
        setComposeContent(launchSource = OFFLINE_ADAPTER)

        moreActionClicked()

        TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SHARE_ACTION.isDisplayedAndCheckClicked()
    }

    private fun moreActionClicked() {
        composeTestRule.onNodeWithTag(testTag = TAG_MENU_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
    }

    private fun String.isDisplayedAndCheckClicked() {
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
                TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION -> VideoPlayerMenuAction.VideoPlayerDownloadAction
                TEST_TAG_VIDEO_PLAYER_SHARE_ACTION -> VideoPlayerMenuAction.VideoPlayerShareAction
                TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION -> VideoPlayerMenuAction.VideoPlayerSendToChatAction
                TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION -> VideoPlayerMenuAction.VideoPlayerGetLinkAction
                TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION -> VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
                TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION -> VideoPlayerMenuAction.VideoPlayerFileInfoAction
                TEST_TAG_VIDEO_PLAYER_RENAME_ACTION -> VideoPlayerMenuAction.VideoPlayerRenameAction
                TEST_TAG_VIDEO_PLAYER_HIDE_ACTION -> VideoPlayerMenuAction.VideoPlayerHideAction
                TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION -> VideoPlayerMenuAction.VideoPlayerUnhideAction
                TEST_TAG_VIDEO_PLAYER_MOVE_ACTION -> VideoPlayerMenuAction.VideoPlayerMoveAction
                TEST_TAG_VIDEO_PLAYER_COPY_ACTION -> VideoPlayerMenuAction.VideoPlayerCopyAction
                TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION -> VideoPlayerMenuAction.VideoPlayerAddToAction
                TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION -> VideoPlayerMenuAction.VideoPlayerRubbishBinAction
                TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION -> VideoPlayerMenuAction.VideoPlayerRemoveAction
                TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION -> VideoPlayerMenuAction.VideoPlayerChatImportAction
                TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION -> VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
                else -> null
            }
        )
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is RUBBISH_BIN_ADAPTER`() {
        setComposeContent(launchSource = RUBBISH_BIN_ADAPTER)
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when isInRubbish is true`() {
        setComposeContent(isInRubbish = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_CHAT and canRemoveFromChat is false`() {
        setComposeContent(launchSource = FROM_CHAT)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_CHAT and canRemoveFromChat is true`() {
        setComposeContent(launchSource = FROM_CHAT, canRemoveFromChat = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FILE_LINK_ADAPTER`() {
        setComposeContent(launchSource = FILE_LINK_ADAPTER)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SHARE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is ZIP_ADAPTER`() {
        setComposeContent(launchSource = ZIP_ADAPTER)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SHARE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FOLDER_LINK_ADAPTER`() {
        setComposeContent(launchSource = FOLDER_LINK_ADAPTER)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_ALBUM_SHARING`() {
        setComposeContent(launchSource = FROM_ALBUM_SHARING)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is VERSIONS_ADAPTER`() {
        setComposeContent(launchSource = VERSIONS_ADAPTER)

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_IMAGE_VIEWER, shouldShowHideNode is false and shouldShowUnhideNode is false`() {
        setComposeContent(
            launchSource = FROM_IMAGE_VIEWER,
            shouldShowHideNode = false,
            shouldShowUnhideNode = false
        )

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION.isNotDisplayed()
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION.isNotDisplayed()
    }

    private fun String.isNotDisplayed() = composeTestRule.onNodeWithTag(
        testTag = this, useUnmergedTree = true
    ).assertIsNotDisplayed()

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_IMAGE_VIEWER, shouldShowHideNode is true and shouldShowUnhideNode is false`() {
        setComposeContent(
            launchSource = FROM_IMAGE_VIEWER,
            shouldShowHideNode = true,
            shouldShowUnhideNode = false
        )

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION.isNotDisplayed()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_IMAGE_VIEWER, shouldShowHideNode is false and shouldShowUnhideNode is true`() {
        setComposeContent(
            launchSource = FROM_IMAGE_VIEWER,
            shouldShowHideNode = false,
            shouldShowUnhideNode = true
        )

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION.isNotDisplayed()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is FROM_IMAGE_VIEWER, shouldShowHideNode is true and shouldShowUnhideNode is true`() {
        setComposeContent(
            launchSource = FROM_IMAGE_VIEWER,
            shouldShowHideNode = true,
            shouldShowUnhideNode = true
        )

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when all parameters are default`() {
        setComposeContent()

        TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION.isDisplayedAndCheckClicked()
        TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_COPY_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerShareAction is displayed when launchSource is default`() {
        setComposeContent(shouldShowShare = true)

        TEST_TAG_VIDEO_PLAYER_SHARE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerGetLinkAction is displayed when launchSource is default`() {
        setComposeContent(shouldShowGetLink = true)

        TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerRemoveLinkAction is displayed when launchSource is default`() {
        setComposeContent(shouldShowRemoveLink = true)

        TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerHideAction is displayed when launchSource is default`() {
        setComposeContent(shouldShowHideNode = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_HIDE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerUnhideAction is displayed when launchSource is default`() {
        setComposeContent(shouldShowUnhideNode = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerRubbishBinAction is displayed when launchSource is default`() {
        setComposeContent(isRubbishBinShown = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that actions are displayed as expected when launchSource is default and isAccess is true`() {
        setComposeContent(isAccess = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_RENAME_ACTION.isDisplayedAndCheckClicked()
        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_MOVE_ACTION.isDisplayedAndCheckClicked()
    }

    @Test
    fun `test that VideoPlayerAddToAction is displayed when shouldShowAddTo is true`() {
        setComposeContent(shouldShowAddTo = true)

        moreActionClicked()
        TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION.isDisplayedAndCheckClicked()
    }
}