package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import java.util.PriorityQueue
import javax.inject.Inject

/**
 * Returns the sync transfers a filter shows in desktop's priority bands: Completing, then active
 * and queued, then paused, then finished. Overflow rows are counted by the store but never
 * listed.
 */
internal class GetSyncTransferListUseCase @Inject constructor(
    private val getSyncTransferRowStateUseCase: GetSyncTransferRowStateUseCase,
    private val getSyncTransferFilterCountsUseCase: GetSyncTransferFilterCountsUseCase,
    private val isSyncTransferSearchMatchUseCase: IsSyncTransferSearchMatchUseCase,
) {

    /**
     * The rows [filter] shows in desktop order, at most [limit] of them, with the true row count
     * and the desktop tab counters.
     */
    operator fun invoke(
        state: SyncTransferStoreState,
        filter: SyncTransferFilter,
        context: SyncTransferRowContext,
        limit: Int = VISIBLE_ROW_LIMIT,
    ): SyncTransferList {
        val active = state.active.values
            .filter { matchesActive(filter, it) }
            .map {
                val rowState = getSyncTransferRowStateUseCase(it, context, directionSpeed = null)
                SyncTransferListItem.Active(it, rowState.status, rowState.isOwnPause)
            }
        // The store keeps completed rows newest first, and they all sort after the active ones.
        val finished = state.completed
            .filter { matchesCompleted(filter, it) }
            .map { SyncTransferListItem.Finished(it) }
        val window = active.smallest(limit, order)
        val items = window + finished.take((limit - window.size).coerceAtLeast(0))
        return SyncTransferList(
            isSeeded = state.isSeeded,
            items = items.toImmutableList(),
            totalCount = active.size + finished.size,
            counts = getSyncTransferFilterCountsUseCase(state, filter),
        )
    }

    /** By band, then priority and tag. */
    private val order: Comparator<SyncTransferListItem.Active> = compareBy(
        { it.band },
        { it.row.entry.priority },
        { it.tag },
    )

    private fun <T> List<T>.smallest(limit: Int, comparator: Comparator<in T>): List<T> {
        if (limit <= 0) return emptyList()
        if (size <= limit) return sortedWith(comparator)
        val heap = PriorityQueue(limit.coerceAtMost(INITIAL_HEAP_CAPACITY), comparator.reversed())
        for (item in this) {
            if (heap.size < limit) {
                heap.add(item)
            } else if (comparator.compare(item, heap.peek()) < 0) {
                heap.poll()
                heap.add(item)
            }
        }
        return heap.sortedWith(comparator)
    }

    private fun matchesActive(filter: SyncTransferFilter, row: SyncTransferRow): Boolean =
        when (filter) {
            SyncTransferFilter.Pending -> true
            is SyncTransferFilter.Direction -> row.entry.direction == filter.direction
            SyncTransferFilter.Completed -> false
            is SyncTransferFilter.Media -> row.mediaType == filter.type
            is SyncTransferFilter.Search -> isSyncTransferSearchMatchUseCase(row.entry, filter)
        }

    private fun matchesCompleted(
        filter: SyncTransferFilter,
        transfer: SyncCompletedTransfer,
    ): Boolean = when (filter) {
        SyncTransferFilter.Pending, is SyncTransferFilter.Direction -> false
        SyncTransferFilter.Completed -> true
        is SyncTransferFilter.Media -> transfer.mediaType == filter.type
        is SyncTransferFilter.Search -> isSyncTransferSearchMatchUseCase(transfer.entry, filter)
    }

    companion object {
        /** The most rows one list carries; a longer list reports its size through `totalCount`. */
        const val VISIBLE_ROW_LIMIT = 500

        private const val INITIAL_HEAP_CAPACITY = 1_024
    }
}

/**
 * The desktop priority band the list sorts by, from the effective status so a paused row moves
 * to the paused band at once. Only the relative value is meaningful.
 */
internal val SyncTransferListItem.band: Int
    get() = when (this) {
        is SyncTransferListItem.Finished -> BAND_FINISHED
        is SyncTransferListItem.Active -> when (status) {
            SyncTransferStatus.Completing -> BAND_COMPLETING
            SyncTransferStatus.Starting,
            SyncTransferStatus.Transferring,
            SyncTransferStatus.Queued,
            SyncTransferStatus.Retrying,
            SyncTransferStatus.OutOfTransferQuota,
            SyncTransferStatus.OutOfStorageSpace,
                -> BAND_ACTIVE

            SyncTransferStatus.Paused, SyncTransferStatus.PausedInQueue -> BAND_PAUSED
            SyncTransferStatus.Completed -> BAND_FINISHED
        }
    }

private const val BAND_COMPLETING = 0
private const val BAND_ACTIVE = 1
private const val BAND_PAUSED = 2
private const val BAND_FINISHED = 3
