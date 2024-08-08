package mega.privacy.android.app.presentation.transfers.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.InProgressActionsBottomSheet

internal const val inProgressActionsModalRoute = "inProgressActionsModal"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.inProgressActionsModal(navHostController: NavHostController) {
    bottomSheet(route = inProgressActionsModalRoute) {
        InProgressActionsBottomSheet(
            onCancelAllTransfers = {
                navHostController.popBackStack(transfersRoute, false)
                navHostController.navigateToCancelAllTransfersDialog()
            }
        )
    }
}

internal fun NavHostController.navigateToInProgressActionsModal(
    navOptions: NavOptions? = null,
) {
    navigate(inProgressActionsModalRoute, navOptions)
}