package mega.privacy.android.app.presentation.transfers.view.navigation.compose

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelAllTransfersDialog

internal const val cancelAllTransfersDialogRoute = "cancelAllTransfers"

internal fun NavGraphBuilder.cancelAllTransfersDialog(navHostController: NavHostController) {
    dialog(route = cancelAllTransfersDialogRoute) { backStackEntry ->
        val viewModel =
            backStackEntry.sharedViewModel<TransfersViewModel>(navController = navHostController)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.inProgressTransfers.isEmpty()) {
            navHostController.popBackStack()
        } else {
            CancelAllTransfersDialog(
                onCancelAllTransfers = viewModel::cancelAllTransfers,
                onDismiss = navHostController::popBackStack,
            )
        }
    }
}

internal fun NavHostController.navigateToCancelAllTransfersDialog(navOptions: NavOptions? = null) {
    navigate(cancelAllTransfersDialogRoute, navOptions)
}