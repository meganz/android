package mega.privacy.android.domain.entity.meeting

/**
 * Scheduled meeting status.
 */
enum class ScheduledMeetingStatus {
    /**
     *  Call not started
     */
    NotStarted,

    /**
     * Call in progress and I am not participating
     */
    NotJoined,

    /**
     * Call in progress and I am participating
     */
    Joined
}