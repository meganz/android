package mega.privacy.android.feature.sync.domain.entity.progress

/** One conflated event of a chunk; [startNotificationNumber] is the chunk's Start for this tag, if any. */
internal data class SyncTransferEvent(
    val type: SyncTransferEventType,
    val entry: SyncTransferEntry,
    val startNotificationNumber: Long?,
)
