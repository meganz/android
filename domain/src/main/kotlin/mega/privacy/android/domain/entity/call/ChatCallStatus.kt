package mega.privacy.android.domain.entity.call

/**
 * Chat call status.
 */
enum class ChatCallStatus {
    /**
     *  Call status Initial
     */
    Initial,

    /**
     *  Call status User no present
     */
    UserNoPresent,

    /**
     * Call status Connecting
     */
    Connecting,

    /**
     * Call status Waiting room
     */
    WaitingRoom,

    /**
     * Call status Joining
     */
    Joining,

    /**
     * Call status In progress
     */
    InProgress,

    /**
     * Call status Terminating user participation
     */
    TerminatingUserParticipation,

    /**
     * Call status Generic Notification
     */
    GenericNotification,

    /**
     * Call status Destroyed
     */
    Destroyed,

    /**
     * Call status Unknown
     */
    Unknown
}

/**
 * Extension function to check if a call is finished
 *
 * @return true if the call is finished, false otherwise
 */
fun ChatCallStatus.isCallFinished() =
    this == ChatCallStatus.Destroyed || this == ChatCallStatus.Unknown
