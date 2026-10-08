package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import javax.inject.Inject

/**
 * Returns the desktop tab counters of the sync transfer store, with the search matches per
 * direction when [SyncTransferFilter.Search] is shown.
 */
internal class GetSyncTransferFilterCountsUseCase @Inject constructor(
    private val isSyncTransferSearchMatchUseCase: IsSyncTransferSearchMatchUseCase,
) {

    operator fun invoke(
        state: SyncTransferStoreState,
        filter: SyncTransferFilter,
    ): SyncTransferFilterCounts {
        val searchMatches = if (filter is SyncTransferFilter.Search) {
            val entries = state.active.values.asSequence().map { it.entry } +
                state.completed.asSequence().map { it.entry }
            entries
                .filter { isSyncTransferSearchMatchUseCase(it, filter, anyDirection = true) }
                .groupingBy { it.direction }
                .eachCount()
        } else {
            emptyMap()
        }
        val counts = state.counts
        return SyncTransferFilterCounts(
            pending = counts.pendingSyncTransfers,
            pendingUploads = counts.lifetimeUploads.pending,
            pendingDownloads = counts.lifetimeDownloads.pending,
            completed = counts.completedTransfers,
            byMediaType = counts.byMediaType,
            searchUploads = searchMatches[SyncTransferDirection.Upload] ?: 0,
            searchDownloads = searchMatches[SyncTransferDirection.Download] ?: 0,
        )
    }
}
