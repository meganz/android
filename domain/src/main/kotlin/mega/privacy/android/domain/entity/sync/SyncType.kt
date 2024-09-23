package mega.privacy.android.domain.entity.sync

/**
 * Enum class representing the different Sync Types mapped from
 * nz.mega.sdk.MegaSync.SyncType
 */
enum class SyncType {
    /**
     * Represents an Unknown Sync Type
     */
    TYPE_UNKNOWN,

    /**
     * Represents a Two-way Sync Type
     */
    TYPE_TWOWAY,

    /**
     * Represents a Backup (one-way) Sync Type
     */
    TYPE_BACKUP
}