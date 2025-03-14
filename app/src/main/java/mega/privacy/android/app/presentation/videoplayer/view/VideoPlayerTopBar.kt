package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar

@Composable
internal fun VideoPlayerTopBar(
    title: String,
    launchSource: Int,
    isInRubbish: Boolean,
    canRemoveFromChat: Boolean,
    nodeIsNull: Boolean,
    shouldShowHideNode: Boolean,
    shouldShowUnhideNode: Boolean,
    shouldShowShare: Boolean,
    shouldShowGetLink: Boolean,
    shouldShowRemoveLink: Boolean,
    isAccess: Boolean,
    isRubbishBinShown: Boolean,
    shouldShowAddTo: Boolean,
    onBackPressed: () -> Unit,
    onMenuActionClicked: (VideoPlayerMenuAction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAppBar(
        modifier = modifier.testTag(VIDEO_PLAYER_TOP_BAR_TEST_TAG),
        title = title,
        appBarType = AppBarType.BACK_NAVIGATION,
        elevation = 0.dp,
        onNavigationPressed = onBackPressed,
        actions = getMenuActions(
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
            isRubbishBinShown = isRubbishBinShown
        ).apply {
            if (shouldShowAddTo) add(VideoPlayerMenuAction.VideoPlayerAddToAction)
        },
        onActionPressed = {
            onMenuActionClicked(it as? VideoPlayerMenuAction)
        }
    )
}

private fun getMenuActions(
    launchSource: Int,
    isInRubbish: Boolean,
    canRemoveFromChat: Boolean,
    nodeIsNull: Boolean,
    shouldShowHideNode: Boolean,
    shouldShowUnhideNode: Boolean,
    shouldShowShare: Boolean,
    shouldShowGetLink: Boolean,
    shouldShowRemoveLink: Boolean,
    isAccess: Boolean,
    isRubbishBinShown: Boolean,
): MutableList<VideoPlayerMenuAction> = when {
    launchSource == OFFLINE_ADAPTER -> mutableListOf(
        VideoPlayerMenuAction.VideoPlayerFileInfoAction,
        VideoPlayerMenuAction.VideoPlayerShareAction,
    )

    launchSource == RUBBISH_BIN_ADAPTER || isInRubbish -> mutableListOf(
        VideoPlayerMenuAction.VideoPlayerFileInfoAction,
        VideoPlayerMenuAction.VideoPlayerRemoveAction,
    )

    launchSource == FROM_CHAT -> {
        mutableListOf<VideoPlayerMenuAction>(
            VideoPlayerMenuAction.VideoPlayerDownloadAction,
            VideoPlayerMenuAction.VideoPlayerChatImportAction,
            VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction,
        ).apply {
            if (canRemoveFromChat) add(VideoPlayerMenuAction.VideoPlayerRemoveAction)
        }
    }

    launchSource == FILE_LINK_ADAPTER || launchSource == ZIP_ADAPTER -> mutableListOf(
        VideoPlayerMenuAction.VideoPlayerDownloadAction,
        VideoPlayerMenuAction.VideoPlayerShareAction,
    )

    launchSource == FOLDER_LINK_ADAPTER
            || launchSource == FROM_ALBUM_SHARING
            || launchSource == VERSIONS_ADAPTER ->
        mutableListOf(VideoPlayerMenuAction.VideoPlayerDownloadAction)

    nodeIsNull -> mutableListOf<VideoPlayerMenuAction>()

    launchSource == FROM_IMAGE_VIEWER -> mutableListOf<VideoPlayerMenuAction>(
        VideoPlayerMenuAction.VideoPlayerDownloadAction,
    ).apply {
        if (shouldShowHideNode) add(VideoPlayerMenuAction.VideoPlayerHideAction)
        if (shouldShowUnhideNode) add(VideoPlayerMenuAction.VideoPlayerUnhideAction)
    }

    else -> mutableListOf<VideoPlayerMenuAction>(
        VideoPlayerMenuAction.VideoPlayerDownloadAction,
        VideoPlayerMenuAction.VideoPlayerFileInfoAction,
        VideoPlayerMenuAction.VideoPlayerSendToChatAction,
        VideoPlayerMenuAction.VideoPlayerCopyAction,
    ).apply {
        if (shouldShowShare) add(VideoPlayerMenuAction.VideoPlayerShareAction)
        if (shouldShowGetLink) add(VideoPlayerMenuAction.VideoPlayerGetLinkAction)
        if (shouldShowRemoveLink) add(VideoPlayerMenuAction.VideoPlayerRemoveLinkAction)
        if (shouldShowHideNode) add(VideoPlayerMenuAction.VideoPlayerHideAction)
        if (shouldShowUnhideNode) add(VideoPlayerMenuAction.VideoPlayerUnhideAction)

        if (isAccess) {
            add(VideoPlayerMenuAction.VideoPlayerRenameAction)
            add(VideoPlayerMenuAction.VideoPlayerMoveAction)
        }
        if (isRubbishBinShown)
            add(VideoPlayerMenuAction.VideoPlayerRubbishBinAction)
    }
}

/**
 * Test tag for video player top bar
 */
const val VIDEO_PLAYER_TOP_BAR_TEST_TAG = "video_player_view:top_bar"