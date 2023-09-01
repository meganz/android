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
     * Access to call from Waiting room, has been allowed for our own user
     */
    WaitingRoomAllow,

    /**
     * Access to call from Waiting room, has been denied for our own user
     */
    WaitingRoomDeny,

    /**
     * Waiting room composition has changed (just for moderators)
     */
    WaitingRoomComposition,

    /**
     * Notify about users that have been pushed into the waiting room  (just for moderators)
     */
    WaitingRoomUsersEntered,

    /**
     * Notify about users that have been left the waiting room
     * (either entered the call or disconnected). (just for moderators)
     */
    WaitingRoomUsersLeave,

    /**
     * Notify about users that have been granted to enter the call. (just for moderators)
     */
    WaitingRoomUsersAllow,

    /**
     * Notify about users that have been denied to enter the call. (just for moderators)
     */
    WaitingRoomUsersDeny,

    /**
     * We have been pushed into a waiting room
     */
    WaitingRoomPushedFromCall,

    /**
     * Unknown
     */
    Unknown
}