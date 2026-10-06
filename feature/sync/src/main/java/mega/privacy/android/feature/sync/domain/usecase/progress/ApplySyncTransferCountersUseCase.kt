package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition
import javax.inject.Inject

/** Desktop's lifetime and batch counters of sync transfers, folded from the store's row transitions. */
internal class ApplySyncTransferCountersUseCase @Inject constructor() {

    /** Applies [transitions] in notificationNumber order, as desktop applies SDK callbacks. */
    operator fun invoke(counts: SyncTransferCounts, transitions: List<SyncTransferTransition>): SyncTransferCounts {
        if (transitions.isEmpty()) return counts
        val media = counts.byMediaType.toPersistentMap().builder()
        var next = counts
        // Conflation leaves survivors out of callback order; a stable sort keeps the caller's
        // order for transitions of the same event.
        transitions.sortedBy { it.notificationNumber }.forEach { next = step(next, it, media) }
        return next.copy(byMediaType = media.build())
    }

    private fun step(
        counts: SyncTransferCounts,
        transition: SyncTransferTransition,
        media: PersistentMap.Builder<SyncMediaType, Int>,
    ): SyncTransferCounts {
        val d = transition.direction
        return when (transition) {
            is SyncTransferTransition.Admitted -> {
                media.add(transition.mediaType, 1)
                counts.withLifetime(d, 1, 1).withBatch(d, 1, 1)
            }

            is SyncTransferTransition.Completed ->
                counts.withLifetime(d, 0, -1).withBatch(d, 0, -1).resetBatchWhenIdle()

            is SyncTransferTransition.RolledBack -> {
                media.add(transition.mediaType, -1)
                counts.withLifetime(d, -1, -1).withBatch(d, -1, -1).resetBatchWhenIdle()
            }

            is SyncTransferTransition.Cleared -> {
                media.add(transition.mediaType, -1)
                val lowered = counts.withLifetime(d, -1, 0)
                if (transition.batchId == counts.batchId) lowered.withBatch(d, -1, 0) else lowered
            }

            // An evicted row can no longer be cleared, but desktop's clear lowers the batch for it.
            is SyncTransferTransition.Evicted -> {
                media.add(transition.mediaType, -1)
                val lowered = counts.withLifetime(d, -1, 0)
                if (transition.batchId == counts.batchId) lowered.withEvicted(d, counts.evicted(d) + 1) else lowered
            }

            is SyncTransferTransition.ClearedEvicted ->
                counts.withBatch(d, -counts.evicted(d), 0).withEvicted(d, 0)
        }
    }

    private fun SyncTransferTally.plus(dTotal: Int, dPending: Int) = SyncTransferTally(
        total = (total + dTotal).coerceAtLeast(0),
        pending = (pending + dPending).coerceAtLeast(0),
    )

    private fun SyncTransferCounts.withLifetime(d: SyncTransferDirection, dTotal: Int, dPending: Int) =
        when (d) {
            SyncTransferDirection.Upload -> copy(lifetimeUploads = lifetimeUploads.plus(dTotal, dPending))
            SyncTransferDirection.Download -> copy(lifetimeDownloads = lifetimeDownloads.plus(dTotal, dPending))
        }

    private fun SyncTransferCounts.withBatch(d: SyncTransferDirection, dTotal: Int, dPending: Int) =
        when (d) {
            SyncTransferDirection.Upload -> copy(batchUploads = batchUploads.plus(dTotal, dPending))
            SyncTransferDirection.Download -> copy(batchDownloads = batchDownloads.plus(dTotal, dPending))
        }

    private fun SyncTransferCounts.evicted(d: SyncTransferDirection) = when (d) {
        SyncTransferDirection.Upload -> batchEvictedUploads
        SyncTransferDirection.Download -> batchEvictedDownloads
    }

    private fun SyncTransferCounts.withEvicted(d: SyncTransferDirection, value: Int) = when (d) {
        SyncTransferDirection.Upload -> copy(batchEvictedUploads = value.coerceAtLeast(0))
        SyncTransferDirection.Download -> copy(batchEvictedDownloads = value.coerceAtLeast(0))
    }

    // Desktop resets the batch on lifetime pending, not the batch's own; the evicted tallies
    // belong to the batch that ended.
    private fun SyncTransferCounts.resetBatchWhenIdle() =
        if (lifetimeUploads.pending == 0 && lifetimeDownloads.pending == 0) {
            copy(
                batchUploads = SyncTransferTally(total = 0, pending = 0),
                batchDownloads = SyncTransferTally(total = 0, pending = 0),
                batchId = batchId + 1,
                batchEvictedUploads = 0,
                batchEvictedDownloads = 0,
            )
        } else {
            this
        }

    private fun PersistentMap.Builder<SyncMediaType, Int>.add(type: SyncMediaType, delta: Int) {
        val count = ((this[type] ?: 0) + delta).coerceAtLeast(0)
        if (count == 0) remove(type) else this[type] = count
    }
}
