package mega.privacy.android.feature.chat.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.chat.dialog.MeetingHasEndedDialog
import mega.privacy.android.navigation.destination.ChatNavKey

/**
 * Meeting has ended dialog destination [NavKey]
 */
@Serializable
data class MeetingHasEndedDialogNavKey(
    val isFromGuest: Boolean,
    val chatId: Long,
) : NavKey


fun EntryProviderScope<NavKey>.meetingHasEndedDialog(
    onBack: () -> Unit,
    onNavigateToChat: (ChatNavKey) -> Unit,
) {
    entry<MeetingHasEndedDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Meeting has ended Dialog"
            )
        )
    ) { key ->
        MeetingHasEndedDialog(
            isFromGuest = key.isFromGuest,
            onDismiss = onBack,
            onShowChat = {
                onNavigateToChat(ChatNavKey(key.chatId, ACTION_CHAT_SHOW_MESSAGES))
            })
    }
}

private const val ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES"