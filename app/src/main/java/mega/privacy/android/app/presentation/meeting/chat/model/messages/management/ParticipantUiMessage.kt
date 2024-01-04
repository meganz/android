package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

/**
 * Participant ui message
 *
 */
abstract class ParticipantUiMessage : ManagementUiChatMessage() {
    /**
     * Handle of action
     */
    abstract val handleOfAction: Long
}