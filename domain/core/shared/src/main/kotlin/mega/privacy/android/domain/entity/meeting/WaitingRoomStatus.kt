package mega.privacy.android.domain.entity.meeting


/**
 * Waiting room status.
 */
enum class WaitingRoomStatus {
    /**
     * Waiting room status allowed
     */
    Allowed,

    /**
     *  Waiting room status not allowed
     */
    NotAllowed,

    /**
     *  Waiting room status unknown
     */
    Unknown,
}