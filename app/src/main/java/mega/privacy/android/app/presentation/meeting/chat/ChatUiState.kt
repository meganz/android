package mega.privacy.android.app.presentation.meeting.chat

/**
 * Chat ui state
 *
 * @property title title of the chat
 * @property isNotificationMute whether notification is mute
 */
data class ChatUiState(
    val title: String? = null,
    val isNotificationMute: Boolean = false,
)