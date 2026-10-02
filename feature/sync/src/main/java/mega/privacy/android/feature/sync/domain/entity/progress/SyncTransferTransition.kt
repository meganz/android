package mega.privacy.android.feature.sync.domain.entity.progress

/** A row change the counters fold, in [notificationNumber] order. */
internal sealed interface SyncTransferTransition {
    val direction: SyncTransferDirection
    val notificationNumber: Long

    /** A tag joined the pending rows. */
    data class Admitted(
        override val direction: SyncTransferDirection,
        val mediaType: SyncMediaType,
        override val notificationNumber: Long,
    ) : SyncTransferTransition

    /** A pending row finished. */
    data class Completed(
        override val direction: SyncTransferDirection,
        override val notificationNumber: Long,
    ) : SyncTransferTransition

    /** A pending row was cancelled or failed, so it leaves the counts. */
    data class RolledBack(
        override val direction: SyncTransferDirection,
        val mediaType: SyncMediaType,
        override val notificationNumber: Long,
    ) : SyncTransferTransition

    /** The user cleared a completed row that finished in [batchId]. */
    data class Cleared(
        override val direction: SyncTransferDirection,
        val mediaType: SyncMediaType,
        override val notificationNumber: Long,
        val batchId: Long,
    ) : SyncTransferTransition

    /** A completed row of [batchId] fell off the completed list; the lifetime count drops it, the batch keeps it. */
    data class Evicted(
        override val direction: SyncTransferDirection,
        val mediaType: SyncMediaType,
        override val notificationNumber: Long,
        val batchId: Long,
    ) : SyncTransferTransition

    /** Clear all also removes the current batch's evicted completions of [direction]. */
    data class ClearedEvicted(
        override val direction: SyncTransferDirection,
        override val notificationNumber: Long,
    ) : SyncTransferTransition
}
