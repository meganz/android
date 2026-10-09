package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition

/**
 * Plans a clear of completed sync transfer rows: those with the given tags, or every row when the
 * tags are null. Only a clear of every row also clears the current batch's evicted completions,
 * as desktop's Clear all does; a clear by tags leaves them.
 */
internal class PlanSyncTransferClearUseCase @Inject constructor() {

    operator fun invoke(
        completed: List<SyncCompletedTransfer>,
        tags: Set<Int>?,
        counts: SyncTransferCounts,
    ): Plan {
        val (removed, kept) = completed.partition { tags == null || it.entry.tag in tags }
        val transitions = removed.mapTo(ArrayList<SyncTransferTransition>()) {
            SyncTransferTransition.Cleared(it.entry.direction, it.mediaType, it.entry.notificationNumber, it.batchId)
        }
        if (tags == null) {
            if (counts.batchEvictedUploads > 0) transitions += clearedEvicted(SyncTransferDirection.Upload)
            if (counts.batchEvictedDownloads > 0) transitions += clearedEvicted(SyncTransferDirection.Download)
        }
        return Plan(kept = if (removed.isNotEmpty()) kept else null, transitions = transitions)
    }

    private fun clearedEvicted(direction: SyncTransferDirection) =
        SyncTransferTransition.ClearedEvicted(direction, CLEARED_EVICTED_ORDER)

    /** The rows a clear keeps, or null when it removes none, and the transitions it emits. */
    class Plan(val kept: List<SyncCompletedTransfer>?, val transitions: List<SyncTransferTransition>)

    private companion object {
        // Stamped last, so an evicted clear settles after every Cleared row of the same input.
        const val CLEARED_EVICTED_ORDER = Long.MAX_VALUE
    }
}
