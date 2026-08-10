package mega.privacy.android.app.presentation.videoplayer.navigation

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.ComposeVideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerLaunchSourceHolder
import mega.privacy.android.app.presentation.videoplayer.view.ComposeVideoPlayerRoute
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * Root NavKey for the Compose video player route. Carries only a short [launchId]; the rich launch
 * payload is held in [VideoPlayerLaunchSourceHolder] to keep the back stack small.
 *
 * The play queue and subtitle selection are NOT separate destinations — they are rendered as
 * in-place overlays by [ComposeVideoPlayerRoute], so all three share the single entry-scoped
 * [ComposeVideoPlayerViewModel] (and therefore one ExoPlayer and one playback state).
 */
@Serializable
@Parcelize
internal data class ComposeVideoPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable

internal fun EntryProviderScope<NavKey>.composeVideoPlayerScreen(
    navigationHandler: NavigationHandler,
    launchSourceHolder: VideoPlayerLaunchSourceHolder,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<ComposeVideoPlayerScreenNavKey> { navKey ->
        val viewModel =
            hiltViewModel<ComposeVideoPlayerViewModel, ComposeVideoPlayerViewModel.Factory>(
                creationCallback = { factory ->
                    val source = launchSourceHolder.consume(navKey.launchId)
                    Timber.d("Compose video player route created (launchId=${navKey.launchId})")
                    factory.create(
                        args = ComposeVideoPlayerViewModel.Args(
                            fileLinkUrl = source?.fileLinkUrl,
                            localFilePath = source?.localFilePath,
                            adapterType = source?.adapterType ?: INVALID_VALUE,
                            handle = source?.handle ?: INVALID_HANDLE,
                            fileName = source?.fileName.orEmpty(),
                            collectionTitle = source?.collectionTitle,
                            collectionId = source?.collectionId,
                        ),
                        initialLaunchSource = source,
                    )
                }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(uiState.nodeSourceType) }
            )
        val nodeActionHandler = rememberSingleNodeActionHandler(
            viewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
        )

        EventEffect(
            event = uiState.invalidLaunchSourceEvent,
            onConsumed = viewModel::onInvalidLaunchSourceConsumed,
            action = { navigationHandler.back() }
        )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            nodeActionHandler = nodeActionHandler,
            onTransfer = onTransfer,
        )

        ComposeVideoPlayerRoute(
            viewModel = viewModel,
            onMoreActionsClicked = {
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = uiState.currentPlayingHandle,
                        nodeSourceType = uiState.nodeSourceType,
                        publicLinkUrl = uiState.fileLinkUrl,
                        localFilePath = uiState.localFilePath,
                        serializedData = uiState.serializedData,
                    )
                )
            },
            onFinish = navigationHandler::back,
        )
    }
}
