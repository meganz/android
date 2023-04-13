package mega.privacy.android.app.presentation.meeting.model

/**
 * Enum class defining the available actions for schedule meeting screen.
 */
enum class ScheduleMeetingAction {

    /**
     * Set frequency.
     */
    Recurrence,

    /**
     * Meeting link.
     */
    MeetingLink,

    /**
     * Add participants to the chat.
     */
    AddParticipants,

    /**
     * Send calendar invite.
     */
    SendCalendarInvite,

    /**
     * Allow non-hosts add participants to the chat.
     */
    AllowNonHostAddParticipants,

    /**
     * Add description to the scheduled meeting.
     */
    AddDescription,
}