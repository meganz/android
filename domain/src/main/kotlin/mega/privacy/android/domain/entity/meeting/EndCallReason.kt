package mega.privacy.android.domain.entity.meeting

/**
 *  End call reason
 */
enum class EndCallReason {
    /**
     *  End call reason invalid
     */
    Invalid,

    /**
     *  End call reason ended
     */
    Ended,

    /**
     *  End call reason rejected
     */
    Rejected,

    /**
     *  End call reason no answer
     */
    NoAnswer,

    /**
     *  End call reason failed
     */
    Failed,

    /**
     *  End call reason cancelled
     */
    Cancelled,

    /**
     *  End call reason by moderator
     */
    ByModerator
}