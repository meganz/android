package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.NoContactToAddDialog

internal fun NavGraphBuilder.noContactToAddDialog(
    navController: NavHostController,
    navigateToInviteContact: () -> Unit,
) {
    dialog(route = "noContactToAdd") {
        NoContactToAddDialog(
            onDismiss = navController::popBackStack,
            onConfirm = navigateToInviteContact
        )
    }
}

internal fun NavHostController.navigateToNoContactToAddDialog(navOptions: NavOptions? = null) {
    navigate("noContactToAdd", navOptions)
}