package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationViewModel
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

private const val isMeetingArg = "isMeeting"

internal fun NavGraphBuilder.mutePushNotificationDialog(
    navController: NavHostController,
) {
    dialog(route = "mutePushNotification/{$isMeetingArg}") { backStackEntry ->
        val graphViewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        val isMeeting =
            backStackEntry.arguments?.getString(isMeetingArg)?.toBooleanStrictOrNull() ?: false
        val onConfirm: (ChatPushNotificationMuteOption) -> Unit =
            { muteOption ->
                graphViewModel.mutePushNotification(muteOption)
                navController.popBackStack()
            }

        val viewModel = hiltViewModel<MutePushNotificationViewModel>()
        val state by
        viewModel.mutePushNotificationUiState.collectAsStateWithLifecycle()

        MutePushNotificationDialog(
            state = state,
            isMeeting = isMeeting,
            onCancel = navController::popBackStack,
            onConfirm = onConfirm
        )
    }
}

internal fun NavHostController.navigateToMutePushNotificationDialog(isMeeting: Boolean, navOptions: NavOptions? = null) {
    navigate("mutePushNotification/$isMeeting", navOptions)
}