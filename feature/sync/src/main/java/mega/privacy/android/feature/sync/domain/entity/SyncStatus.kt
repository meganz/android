package mega.privacy.android.feature.sync.domain.entity

/**
 * Enum class representing the status of the sync
 */
enum class SyncStatus {
    /**
     * SYNCING - the engine is actively syncing or scanning the folder
     */
    SYNCING,

    /**
     * SYNCED - the engine is not syncing or scanning the folder
     */
    SYNCED,

    /**
     * PAUSED - the engine is paused by the user
     */
    PAUSED,

    /**
     * ERROR - the engine is paused due to an error
     */
    ERROR,

    /**
     * DISABLED - the engine is disabled by the user (specific for CU and MU)
     */
    DISABLED,
}