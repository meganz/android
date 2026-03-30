package mega.privacy.android.feature.clouddrive.presentation.audio

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AudioNavKey

/**
 * Registers the Compose Audio destination for [AudioNavKey].
 * Feature-flag routing (Compose vs legacy activity) lives in the app module on `AudioSectionNavKey`.
 */
fun EntryProviderScope<NavKey>.audioScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AudioNavKey> {
        val viewModel = hiltViewModel<AudioViewModel>()
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
            )

        AudioScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onTransfer = onTransfer,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )
    }
}

