package mega.privacy.android.feature.chat.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.chat.dialog.MeetingHasEndedDialog
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.destination.ChatNavKey

/**
 * Meeting has ended dialog destination [DialogNavKey]
 */
@Serializable
data class MeetingHasEndedDialogNavKey(
    val chatId: Long?,
) : NoSessionNavKey.Optional, DialogNavKey

data object MeetingHasEndedDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            meetingHasEndedDialog(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.meetingHasEndedDialog(
    navigateBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<MeetingHasEndedDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Meeting has ended Dialog"
            )
        )
    ) { key ->
        MeetingHasEndedDialog(
            doesChatExist = key.chatId != null,
            onDismiss = {
                navigateBack()
                onDialogHandled()
            },
            onShowChat = {
                key.chatId?.let {
                    navigate(ChatNavKey(it, ACTION_CHAT_SHOW_MESSAGES))
                }
            },
        )
    }
}

internal const val ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES"