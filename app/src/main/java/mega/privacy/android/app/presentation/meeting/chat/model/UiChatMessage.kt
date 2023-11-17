package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Ui chat message
 * @property time Time of the message
 * @property isMe True if the message is from me, false otherwise
 */
sealed interface UiChatMessage {
    // define common properties here time, avatar, isMe, ...
    val time: Long

    val isMe: Boolean
}