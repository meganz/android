package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import javax.inject.Inject

/**
 * Returns whether a sync transfer's file name contains the search text, ignoring case, in the
 * search's direction when it has one.
 */
internal class IsSyncTransferSearchMatchUseCase @Inject constructor() {

    /**
     * Whether [entry] matches [search]; [anyDirection] ignores the search's direction, as the
     * per-direction counters do.
     */
    operator fun invoke(
        entry: SyncTransferEntry,
        search: SyncTransferFilter.Search,
        anyDirection: Boolean = false,
    ): Boolean =
        (anyDirection || search.direction == null || entry.direction == search.direction) &&
            entry.fileName.contains(search.text, ignoreCase = true)
}
