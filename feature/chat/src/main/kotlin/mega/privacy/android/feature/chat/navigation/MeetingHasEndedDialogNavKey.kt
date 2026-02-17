package mega.privacy.android.feature.chat.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.chat.meeting.view.MeetingHasEndedDialog
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.LeftMeetingNavKey

/**
 * Meeting has ended dialog destination [DialogNavKey]
 */
@Serializable
data class MeetingHasEndedDialogNavKey(
    val chatId: Long?,
) : NoSessionNavKey.Optional, DialogNavKey

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
                } ?: navigate(LeftMeetingNavKey())
            },
        )
    }
}

internal const val ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES"