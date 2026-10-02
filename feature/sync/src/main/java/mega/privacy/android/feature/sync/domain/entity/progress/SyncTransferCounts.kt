package mega.privacy.android.feature.sync.domain.entity.progress

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

/** How many transfers a counter currently counts, and how many of them are still pending. */
internal data class SyncTransferTally(val total: Int, val pending: Int) {
    val completed: Int get() = total - pending
}

/** A batch's progress as (ongoing + completed) over total. */
internal data class SyncTransferBatchProgress(val current: Int, val total: Int)

/** Desktop's lifetime and batch counters of sync transfers, and lifetime counts per media type. */
internal data class SyncTransferCounts(
    val lifetimeUploads: SyncTransferTally,
    val lifetimeDownloads: SyncTransferTally,
    val batchUploads: SyncTransferTally,
    val batchDownloads: SyncTransferTally,
    val batchId: Long,
    val byMediaType: ImmutableMap<SyncMediaType, Int>,
    // Evicting a completed row must not shrink the batch, but a later Clear all must still remove it.
    val batchEvictedUploads: Int = 0,
    val batchEvictedDownloads: Int = 0,
) {
    val pendingSyncTransfers: Int get() = lifetimeUploads.pending + lifetimeDownloads.pending
    val completedTransfers: Int get() = lifetimeUploads.completed + lifetimeDownloads.completed

    companion object {
        /** The counters before any sync transfer. */
        val Empty: SyncTransferCounts = SyncTransferCounts(
            lifetimeUploads = SyncTransferTally(total = 0, pending = 0),
            lifetimeDownloads = SyncTransferTally(total = 0, pending = 0),
            batchUploads = SyncTransferTally(total = 0, pending = 0),
            batchDownloads = SyncTransferTally(total = 0, pending = 0),
            batchId = 0,
            byMediaType = persistentMapOf(),
        )
    }
}
