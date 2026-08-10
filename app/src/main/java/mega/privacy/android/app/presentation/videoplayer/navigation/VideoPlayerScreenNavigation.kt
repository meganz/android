package mega.privacy.android.app.presentation.videoplayer.navigation

import android.app.Activity
import android.os.Parcelable
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerScreen
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.ACTION_PENDING_DEEP_LINK
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.shared.nodes.sheet.PublicLinkAuthAlertBottomSheet
import mega.privacy.android.shared.nodes.sheet.PublicLinkType

@Serializable
internal data object VideoPlayerScreenNavKey : NavKey

@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.videoPlayerScreen(
    navigationHandler: NavigationHandler,
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    navigate: (NavKey) -> Unit,
    onMoreActionsClicked: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onEnterPip: () -> Unit,
) {
    entry<VideoPlayerScreenNavKey> {
        val activity = LocalActivity.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var showLoginRequiredSheet by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            handleAutoReplayIfPaused()
        }

        LaunchedEffect(uiState.navigateToSelectSubtitleScreen) {
            if (uiState.navigateToSelectSubtitleScreen) {
                navigate(VideoPlayerSelectSubtitleScreenNavKey)
                viewModel.updateNavigateToSelectSubtitle(false)
            }
        }

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(viewModel.uiState.value.nodeSourceType) }
            )
        val nodeActionHandler = rememberSingleNodeActionHandler(
            viewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
        )
        val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            nodeActionHandler = nodeActionHandler,
            onTransfer = onTransfer,
        )

        // Logged-out "Save to MEGA" raises loginRequiredEvent; prompt sign in / sign up.
        EventEffect(
            event = nodeActionState.loginRequiredEvent,
            onConsumed = nodeOptionsActionViewModel::resetLoginRequiredEvent,
        ) {
            showLoginRequiredSheet = true
        }

        if (showLoginRequiredSheet) {
            // Both folder links and album sharing map to FOLDER_LINK; album sharing is
            // distinguished via isAlbumSharingLink for correct PublicLinkType display.
            val isFolderLink = uiState.nodeSourceType == NodeSourceType.FOLDER_LINK
            val isAlbumLink = uiState.isAlbumSharingLink
            val publicLinkType = when {
                isAlbumLink -> PublicLinkType.Album
                isFolderLink -> PublicLinkType.Folder
                else -> PublicLinkType.File
            }
            PublicLinkAuthAlertBottomSheet(
                type = publicLinkType,
                onSignupClicked = {
                    showLoginRequiredSheet = false
                    uiState.fileLinkUrl?.let { url ->
                        if (!isFolderLink) viewModel.armPendingPreviewAutoOpen()
                        activity.launchAuthForFileLink(url, CreateAccountNavKey())
                    }
                },
                onLoginClicked = {
                    showLoginRequiredSheet = false
                    uiState.fileLinkUrl?.let { url ->
                        if (!isFolderLink) viewModel.armPendingPreviewAutoOpen()
                        activity.launchAuthForFileLink(url, LoginNavKey())
                    }
                },
                onDismissSheet = { showLoginRequiredSheet = false },
            )
        }

        VideoPlayerScreen(
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = {
                navigate(VideoPlayerQueueScreenNavKey)
            },
            onMoreActionsClicked = onMoreActionsClicked,
            onRetry = onRetry,
            onFinish = onFinish,
            onEnterPip = onEnterPip,
            navigateToTransfers = { navigationHandler.navigate(TransfersNavKey()) },
        )
    }
}

/**
 * Open [MegaActivity] at the requested auth screen, stashing the file link as a pending deep link
 * so it reopens after sign in. The standalone player has no auth destinations of its own.
 */
private fun <T> Activity?.launchAuthForFileLink(
    fileLinkUrl: String,
    authDestination: T,
) where T : NavKey, T : Parcelable {
    val activity = this ?: return
    val intent = MegaActivity.getIntentWithExtraDestinations(activity, listOf(authDestination))
        .apply {
            action = ACTION_PENDING_DEEP_LINK
            data = fileLinkUrl.toUri()
        }
    activity.startActivity(intent)
    // Authentication happens in MegaActivity, which reopens the file link and this video.
    activity.finish()
}
