package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.OfflineInfoNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderScope<NavKey>.offlineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
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
            onTransfer = onTransfer,
            openFileInformation = openFileInformation
        )
    }
}

fun EntryProviderScope<NavKey>.offlineInfoScreen(
    removeDestination: () -> Unit,
) {
    entry<OfflineInfoNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            context.megaNavigator.openOfflineFileInfoActivity(
                context = context,
                handle = args.handle
            )
            removeDestination()
        }
    }
}