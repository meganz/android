package mega.privacy.android.domain.entity.meeting

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
     * Call status Destroyed
     */
    Destroyed
}