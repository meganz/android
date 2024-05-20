package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.AllContactsAddedDialog

internal fun NavGraphBuilder.allParticipantsDialog(
    navController: NavHostController,
) {
    dialog(route = "allContactsParticipate") {
        AllContactsAddedDialog(
            onDismiss = { navController.popBackStack() },
        )
    }
}

internal fun NavHostController.navigateToAllParticipantsDialog(navOptions: NavOptions? = null) {
    navigate("allContactsParticipate", navOptions)
}