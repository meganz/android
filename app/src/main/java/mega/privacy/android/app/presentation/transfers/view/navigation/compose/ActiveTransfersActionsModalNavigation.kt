package mega.privacy.android.app.presentation.transfers.view.navigation.compose

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.app.presentation.transfers.view.sheet.ActiveTransfersActionsBottomSheet

internal const val activeTransfersActionsModalRoute = "activeTransfersActionsModal"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.activeTransfersActionsModal(navHostController: NavHostController) {
    bottomSheet(route = activeTransfersActionsModalRoute) { backStackEntry ->
        val viewModel =
            backStackEntry.sharedViewModel<TransfersViewModel>(navController = navHostController)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.activeTransfers.isEmpty()) {
            navHostController.popBackStack(transfersRoute, false)
        } else {
            ActiveTransfersActionsBottomSheet(
                onCancelAllTransfers = {
                    navHostController.popBackStack(transfersRoute, false)
                    navHostController.navigateToCancelAllTransfersDialog()
                }
            )
        }
    }
}

internal fun NavHostController.navigateToActiveTransfersActionsModal(
    navOptions: NavOptions? = null,
) {
    navigate(activeTransfersActionsModalRoute, navOptions)
}