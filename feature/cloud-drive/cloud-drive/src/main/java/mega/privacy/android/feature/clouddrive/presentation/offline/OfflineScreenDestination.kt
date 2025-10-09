package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.destination.OfflineNavKey

fun NavGraphBuilder.offlineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    composable<OfflineNavKey> { backStackEntry ->
        val args = backStackEntry.toRoute<OfflineNavKey>()
        val viewModel = hiltViewModel<OfflineViewModel>(key = args.nodeId.toString())

        OfflineScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
            onTransfer = onTransfer
        )
    }
}