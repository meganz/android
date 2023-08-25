package mega.privacy.android.domain.entity.meeting

/**
 * Chat call changes.
 */
enum class ChatCallChanges {
    /**
     *  Change type Status
     */
    Status,

    /**
     *  Change type Local AV flags
     */
    LocalAVFlags,

    /**
     * Change type Ringing status
     */
    RingingStatus,

    /**
     * Change type Call composition
     */
    CallComposition,

    /**
     * Change type On hold
     */
    OnHold,

    /**
     * Change type Speaker
     */
    Speaker,

    /**
     * Change type Audio level
     */
    AudioLevel,

    /**
     * Change type Network quality
     */
    NetworkQuality,

    /**
     * Change type Outgoing ringing stop
     */
    OutgoingRingingStop,

    /**
     * Change type Waiting room allow
     */
    WRAllow,

    /**
     * Change type Waiting room deny
     */
    WRDeny,

    /**
     * Change type Waiting room composition
     */
    WRComposition,

    /**
     * Change type Waiting room users entered
     */
    WRUsersEntered,

    /**
     * Change type Waiting room users leave
     */
    WRUsersLeave,

    /**
     * Change type Waiting room users allow
     */
    WRUsersAllow,

    /**
     * Change type Waiting room users deny
     */
    WRUsersDeny,

    /**
     * Change type Waiting room pushed from call
     */
    WRPushedFromCall,

    /**
     * Unknown
     */
    Unknown
}