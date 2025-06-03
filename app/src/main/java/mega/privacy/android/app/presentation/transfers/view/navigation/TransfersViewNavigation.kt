package mega.privacy.android.app.presentation.transfers.view.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.TransfersView

@Serializable
class TransfersInfo(
    val tabIndex: Int = ACTIVE_TAB_INDEX,
)

internal fun NavGraphBuilder.transfersScreen(
    onBackPress: () -> Unit,
    onNavigateToStorageSettings: () -> Unit,
) {
    composable<TransfersInfo> { backStackEntry ->
        val viewModel = hiltViewModel<TransfersViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TransfersView(
            onBackPress = onBackPress,
            uiState = uiState,
            onTabSelected = viewModel::updateSelectedTab,
            onPlayPauseTransfer = viewModel::playOrPauseTransfer,
            onResumeTransfers = viewModel::resumeTransfersQueue,
            onPauseTransfers = viewModel::pauseTransfersQueue,
            onRetryFailedTransfers = viewModel::retryAllFailedTransfers,
            onCancelAllFailedTransfers = viewModel::cancelAllTransfers,
            onClearAllFailedTransfers = viewModel::clearAllFailedTransfers,
            onClearAllCompletedTransfers = viewModel::clearAllCompletedTransfers,
            onActiveTransfersReorderPreview = viewModel::onActiveTransfersReorderPreview,
            onActiveTransfersReorderConfirmed = viewModel::onActiveTransfersReorderConfirmed,
            onConsumeStartEvent = viewModel::consumeStartEvent,
            onNavigateToStorageSettings = onNavigateToStorageSettings,
            onSelectActiveTransfers = viewModel::startActiveTransfersSelection,
            onActiveTransferSelected = viewModel::toggleActiveTransferSelected,
            onSelectAllActiveTransfers = viewModel::selectAllActiveTransfers,
            onSelectTransfersClose = viewModel::stopTransfersSelection,
            onCancelSelectedActiveTransfers = viewModel::cancelSelectedActiveTransfers,
            onCompletedTransferSelected = viewModel::toggleCompletedTransferSelection,
            onSelectAllCompletedTransfers = viewModel::selectAllCompletedTransfers,
            onClearSelectedCompletedTransfers = viewModel::clearSelectedCompletedTransfers,
            onSelectCompletedTransfers = viewModel::startCompletedTransfersSelection
        )
    }
}