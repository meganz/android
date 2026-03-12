package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.OfflineNavKey

fun EntryProviderScope<NavKey>.offlineScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onNavigateToTransfers: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    openFileInformation: (String) -> Unit,
) {
    entry<OfflineNavKey> { args ->
        val viewModel = hiltViewModel<OfflineViewModel, OfflineViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(args)
            }
        )

        OfflineScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
            onNavigateToTransfers = onNavigateToTransfers,
            onTransfer = onTransfer,
            openFileInformation = openFileInformation,
            onNavigate = navigationHandler::navigate,
        )
    }
}
