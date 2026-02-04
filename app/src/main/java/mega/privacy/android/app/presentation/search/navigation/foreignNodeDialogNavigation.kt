package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.resources.R as sharedResR

internal fun NavGraphBuilder.foreignNodeDialogNavigation(navHostController: NavHostController) {
    dialog(searchForeignNodeDialog) {
        MegaAlertDialog(
            text = stringResource(id = sharedResR.string.incoming_share_storage_quota_warning_message),
            confirmButtonText = stringResource(id = sharedResR.string.general_ok),
            cancelButtonText = null,
            onConfirm = { navHostController.navigateUp() },
            onDismiss = { navHostController.navigateUp() },
            dismissOnClickOutside = false
        )
    }
}

internal const val searchForeignNodeDialog = "search/foreign_node_dialog"