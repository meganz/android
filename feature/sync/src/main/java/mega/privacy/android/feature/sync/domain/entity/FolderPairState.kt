package mega.privacy.android.feature.sync.domain.entity

/**
 * Indicates the status of the Sync
 */
enum class FolderPairState {
    /**
     * Sync config has loaded but we have not attempted to start it yet
     */
    PENDING,

    /**
     * Sync DB is in the process of loading from disk
     */
    LOADING,

    /**
     * Sync DB is loaded and active
     */
    RUNNING,

    /**
     * Sync DB is loaded but sync logic is suspended for now (useful for debugging)
     */
    PAUSED,

    /**
     * Sync DB is not loaded, but it is on disk with the last known sync state.
     */
    SUSPENDED,

    /**
     * Sync DB does not exist.  Starting it is like configuring a brand new sync with those settings.
     */
    DISABLED;

    companion object {
        /**
         * Get an enum class by its position
         */
        fun getByOrdinal(value: Int) = values().first { it.ordinal == value }
    }
}
