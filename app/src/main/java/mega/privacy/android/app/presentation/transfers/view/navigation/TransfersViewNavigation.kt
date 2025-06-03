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
            onClearAllCompletedTransfers = viewModel::clearAllCompletedTransfers,
            onClearAllFailedTransfers = viewModel::clearAllFailedTransfers,
            onActiveTransfersReorderPreview = viewModel::onActiveTransfersReorderPreview,
            onActiveTransfersReorderConfirmed = viewModel::onActiveTransfersReorderConfirmed,
            onConsumeStartEvent = viewModel::consumeStartEvent,
            onNavigateToStorageSettings = onNavigateToStorageSettings,
            onSelectActiveTransfers = viewModel::startActiveTransfersSelection,
            onSelectCompletedTransfers = viewModel::startCompletedTransfersSelection,
            onSelectFailedTransfers = viewModel::startFailedTransfersSelection,
            onSelectTransfersClose = viewModel::stopTransfersSelection,
            onActiveTransferSelected = viewModel::toggleActiveTransferSelected,
            onCompletedTransferSelected = viewModel::toggleCompletedTransferSelection,
            onFailedTransferSelected = viewModel::toggleFailedTransferSelection,
            onCancelSelectedActiveTransfers = viewModel::cancelSelectedActiveTransfers,
            onClearSelectedCompletedTransfers = viewModel::clearSelectedCompletedTransfers,
            onClearSelectedFailedTransfers = viewModel::clearSelectedFailedTransfers,
            onRetrySelectedFailedTransfers = viewModel::retrySelectedFailedTransfers,
            onSelectAllActiveTransfers = viewModel::selectAllActiveTransfers,
            onSelectAllCompletedTransfers = viewModel::selectAllCompletedTransfers,
            onSelectAllFailedTransfers = viewModel::selectAllFailedTransfers,
        )
    }
}