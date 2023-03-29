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
     * Unknown
     */
    Unknown
}