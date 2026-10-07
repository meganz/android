package mega.privacy.android.feature.sync.domain.entity.progress

/** What a sync transfer row is doing, as the user should see it. */
internal enum class SyncTransferStatus {
    Starting,
    Transferring,
    Queued,
    OutOfStorageSpace,
    OutOfTransferQuota,
    Paused,
    PausedInQueue,
    Retrying,
    Completing,
    Completed,
}
