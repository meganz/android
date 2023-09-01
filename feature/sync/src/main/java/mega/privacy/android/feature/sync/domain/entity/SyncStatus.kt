package mega.privacy.android.feature.sync.domain.entity

/**
 * Enum class representing the status of the sync
 */
enum class SyncStatus {
    MONITORING,
    SYNCING,
    SYNCED,
    ERROR
}