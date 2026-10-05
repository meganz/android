package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * A sync's run state as the SDK reports it (MegaSync.getRunState).
 */
internal enum class SyncRunState {
    /**
     * The sync is waiting to start
     */
    Pending,

    /**
     * The sync is loading its state
     */
    Loading,

    /**
     * The sync is running
     */
    Running,

    /**
     * The sync is suspended, by the user or by a condition
     */
    Suspended,

    /**
     * The sync is disabled
     */
    Disabled,
}
