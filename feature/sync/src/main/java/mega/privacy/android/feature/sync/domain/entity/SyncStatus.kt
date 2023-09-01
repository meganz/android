package mega.privacy.android.feature.sync.domain.entity

/**
 * Enum class representing the status of the sync
 *
 * SYNCING - the engine is actively syncing or scanning the folder
 * SYNCED - the engine is not syncing or scanning the folder
 * PAUSED - the engine is paused by the user
 * ERROR - the engine is paused due to an error
 */
enum class SyncStatus {
    SYNCING,
    SYNCED,
    PAUSED,
    ERROR
}