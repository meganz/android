package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog

internal fun NavGraphBuilder.foreignNodeDialogNavigation(navHostController: NavHostController) {
    dialog(searchForeignNodeDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.warning_share_owner_storage_quota),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { navHostController.navigateUp() },
            onDismiss = { navHostController.navigateUp() },
            dismissOnClickOutside = false
        )
    }
}

internal const val searchForeignNodeDialog = "search/foreign_node_dialog"