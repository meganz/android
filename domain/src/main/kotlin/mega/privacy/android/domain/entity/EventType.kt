package mega.privacy.android.domain.entity

/**
 * Type of [Event]
 */
enum class EventType {

    /**
     * CommitDb
     */
    CommitDb,

    /**
     * AccountConfirmation
     */
    AccountConfirmation,

    /**
     * ChangeToHttps
     */
    ChangeToHttps,

    /**
     * Disconnect
     */
    Disconnect,

    /**
     * AccountBlocked
     */
    AccountBlocked,

    /**
     * Storage
     */
    Storage,

    /**
     * NodesCurrent
     */
    NodesCurrent,

    /**
     * MediaInfoReady
     */
    MediaInfoReady,

    /**
     * StorageSumChanged
     */
    StorageSumChanged,

    /**
     * BusinessStatus
     */
    BusinessStatus,

    /**
     * KeyModified
     */
    KeyModified,

    /**
     * MiscFlagsReady
     */
    MiscFlagsReady,

    /**
     * Other unknown even types
     */
    Unknown,


}