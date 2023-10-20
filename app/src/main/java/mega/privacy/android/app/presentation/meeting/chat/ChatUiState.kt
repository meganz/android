package mega.privacy.android.app.presentation.meeting.chat

/**
 * Chat ui state
 *
 * @property title title of the chat
 * @property isChatNotificationMute whether notification is mute
 */
data class ChatUiState(
    val title: String? = null,
    val isChatNotificationMute: Boolean = false,
)