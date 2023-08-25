package mega.privacy.android.domain.entity.meeting

/**
 *  Term Code type
 */
enum class TermCodeType {
    /**
     *  Term code invalid
     */
    Invalid,

    /**
     *  Term code Hangup
     */
    Hangup,

    /**
     *  Term code too many participants
     */
    TooManyParticipants,

    /**
     *  Term code reject
     */
    Reject,

    /**
     *  Term code Error
     */
    Error,

    /**
     *  Term code no participate
     */
    NoParticipate,

    /**
     *  Term code too many clients
     */
    TooManyClients,

    /**
     *  Term code protocol version
     */
    ProtocolVersion,

    /**
     *  Term code kicked
     */
    Kicked,

    /**
     *  Term code Unknown
     */
    Unknown,
}