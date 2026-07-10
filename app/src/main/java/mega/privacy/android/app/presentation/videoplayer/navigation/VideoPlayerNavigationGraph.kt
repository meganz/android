package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

internal fun EntryProviderScope<NavKey>.videoPlayerEntryProvider(
    navigationHandler: NavigationHandler,
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onEnterPip: () -> Unit,
) {
    videoPlayerScreen(
        navigationHandler = navigationHandler,
        viewModel = viewModel,
        player = player,
        handleAutoReplayIfPaused = handleAutoReplayIfPaused,
        navigate = {
            navigationHandler.navigate(it)
        },
        onTransfer = onTransfer,
        onRetry = onRetry,
        onFinish = onFinish,
        onEnterPip = onEnterPip,
        onMoreActionsClicked = {
            val uiState = viewModel.uiState.value
            navigationHandler.navigate(
                NodeOptionsBottomSheetNavKey(
                    nodeHandle = uiState.currentPlayingHandle,
                    nodeSourceType = uiState.nodeSourceType,
                    publicLinkUrl = uiState.fileLinkUrl,
                    localFilePath = uiState.localFilePath,
                    serializedData = uiState.serializedData,
                    chatId = uiState.chatId,
                    msgId = uiState.msgId,
                    partiallyExpand = uiState.nodeSourceType.shouldPartiallyExpand,
                )
            )
        },
    )

    videoPlayerQueueScreen(
        viewModel = viewModel,
        onBack = navigationHandler::back,
    )

    videoPlayerSelectSubtitleScreen(
        viewModel = viewModel,
        onBack = navigationHandler::back,
    )
}

/**
 * These source types have few node options. Skipping partial expansion avoids a visual
 * collapse animation where the bottom sheet first opens at half-screen (driven by the
 * loading skeleton that shows more items) and then shrinks down to fit the actual options.
 */
private val NodeSourceType.shouldPartiallyExpand: Boolean
    get() = when (this) {
        NodeSourceType.CHAT,
        NodeSourceType.FILE_LINK,
        NodeSourceType.FOLDER_LINK,
        NodeSourceType.VIDEO_PLAYER_VERSIONS,
        NodeSourceType.VIDEO_PLAYER_ZIP_FILE,
        NodeSourceType.VIDEO_PLAYER_IMAGE_VIEWER,
            -> false

        else -> true // new source types default to partially expanded
    }
