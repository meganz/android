package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition

/**
 * Settles one input's transitions into the state's counters, one at a time in notification order,
 * and puts its finished rows, newest first, in front of the completed list, cut at the limit.
 * Every cut row is evicted with its own batch id, which never lowers the batch.
 */
internal class SettleSyncTransferCompletionsUseCase @Inject constructor(
    private val applySyncTransferCountersUseCase: ApplySyncTransferCountersUseCase,
) {

    operator fun invoke(
        state: SyncTransferStoreState,
        journal: List<Emission>,
        nowMillis: Long,
        completedLimit: Int,
    ): SyncTransferStoreState {
        if (journal.isEmpty()) return state
        var settledCounts = state.counts
        val finished = ArrayList<SyncCompletedTransfer>()
        // Each transition is applied alone so a completed row takes the batch it finished in.
        for (item in journal.sortedBy { it.transition.notificationNumber }) {
            val completion = item.completion
            if (completion != null) {
                finished += SyncCompletedTransfer(
                    entry = completion.entry,
                    mediaType = completion.mediaType,
                    finishedAtMillis = nowMillis,
                    batchId = settledCounts.batchId,
                )
            }
            settledCounts = applySyncTransferCountersUseCase(settledCounts, listOf(item.transition))
        }
        // A copy keeps the completed list instance, so an input that finishes nothing publishes the same list.
        if (finished.isEmpty()) return state.copy(counts = settledCounts)
        val all = finished.sortedByDescending { it.entry.notificationNumber } + state.completed
        val evicted = all.drop(completedLimit)
        if (evicted.isNotEmpty()) {
            settledCounts = applySyncTransferCountersUseCase(
                settledCounts,
                evicted.map {
                    SyncTransferTransition.Evicted(
                        it.entry.direction,
                        it.mediaType,
                        it.entry.notificationNumber,
                        it.batchId,
                    )
                },
            )
        }
        return state.copy(completed = all.take(completedLimit).toPersistentList(), counts = settledCounts)
    }

    /** The finished entry and media type a completed row is built from. */
    class Completion(val entry: SyncTransferEntry, val mediaType: SyncMediaType)

    /** A transition the input emitted, with the completed row it adds when it is a completion. */
    class Emission(val transition: SyncTransferTransition, val completion: Completion? = null)
}
