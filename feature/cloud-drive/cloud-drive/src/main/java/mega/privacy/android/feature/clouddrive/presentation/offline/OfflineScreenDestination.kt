package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.destination.OfflineNavKey

fun EntryProviderBuilder<NavKey>.offlineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
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
            onTransfer = onTransfer
        )
    }
}