package mega.privacy.android.domain.entity

/**
 * Enum class that provides different types of Backup States
 *
 * @param value The value of the Backup State to be sent to the API
 */
enum class BackupState(val value: Int) {

    /**
     * Invalid Backup State
     */
    INVALID(-1),

    /**
     * An uninitialized Backup State
     */
    NOT_INITIALIZED(0),

    /**
     * Active Backup State (Enabled)
     */
    ACTIVE(1),

    /**
     * Failed Backup State (Permanently Disabled)
     */
    FAILED(2),

    /**
     * Temporarily Disabled Backup State, sent when a transient situation occurs
     * (e.g. Account Blocked)
     */
    TEMPORARILY_DISABLED(3),

    /**
     * Disabled Backup State
     */
    DISABLED(4),

    /**
     * Pause Uploads Backup State, sent when Upload Transfers are paused in the SDK
     */
    PAUSE_UPLOADS(5),

    /**
     * Pause Downloads Backup State, sent when Download Transfers are paused in the SDK
     */
    PAUSE_DOWNLOADS(6),

    /**
     * Pause All Backup State, sent when both Upload and Download Transfers are paused in the SDK
     */
    PAUSE_ALL(7),

    /**
     * Deleted Backup State, sent when the user wants to delete the Backup via Backup Center
     */
    DELETED(8);

    companion object {
        private val map = values().associateBy(BackupState::value)

        /**
         * Performs a Reverse Lookup in order to retrieve the correct [BackupState]
         * depending on the value that was passed
         *
         * @param value The [BackupState] value
         * @return The appropriate [BackupState], or [INVALID] if there is no matching value
         */
        fun fromValue(value: Int) = map[value] ?: INVALID
    }
}