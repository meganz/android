package mega.privacy.android.domain.entity.meeting

/**
 * Meeting participant call status
 */
enum class MeetingParticipantNotInCallStatus {
    /**
     * "Not in call" status
     */
    NotInCall,

    /**
     * "Calling" status
     */
    Calling,

    /**
     * "No response" status
     */
    NoResponse
}