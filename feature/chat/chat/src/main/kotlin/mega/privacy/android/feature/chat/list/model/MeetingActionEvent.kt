package mega.privacy.android.feature.chat.list.model

/**
 * Meeting action picked from the chat list meetings bottom sheet.
 */
internal enum class MeetingAction {
    Start,
    Join,
    Schedule,
}

/**
 * Outcome of a picked [MeetingAction], resolved by the view model so the hosting entry only has to
 * navigate or surface the block.
 */
internal sealed interface MeetingActionEvent {

    /**
     * Start an unscheduled meeting immediately.
     */
    data object StartMeeting : MeetingActionEvent

    /**
     * Ask the user for the link of the meeting they want to join.
     */
    data object JoinMeeting : MeetingActionEvent

    /**
     * Open the schedule meeting screen.
     */
    data object ScheduleMeeting : MeetingActionEvent

    /**
     * The user is already in a call, so a new meeting cannot be started.
     */
    data object StartBlockedByOngoingCall : MeetingActionEvent

    /**
     * The user is already in a call, so another one cannot be joined.
     */
    data object JoinBlockedByOngoingCall : MeetingActionEvent
}
