package mega.privacy.android.domain.entity.call

/**
 *  Term Code type
 */
enum class ChatCallTermCodeType {
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
     *  Term code Waiting Room timeout
     */
    WaitingRoomTimeout,

    /**
     *  Free plan limitations. Call duration exceeded for call
     */
    CallDurationLimit,

    /**
     *  Free plan limitations. Call max different users exceeded for call
     */
    CallUsersLimit,

    /**
     *  Term code Unknown
     */
    Unknown,
}