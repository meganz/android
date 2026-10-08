package mega.privacy.android.feature.sync.domain.entity.progress

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

/** One row of a sync transfer list, identified by its transfer [tag]. */
internal sealed interface SyncTransferListItem {
    val tag: Int

    /** A pending row with its effective [status]; [isOwnPause] ignores the app-wide pause. */
    data class Active(
        val row: SyncTransferRow,
        val status: SyncTransferStatus,
        val isOwnPause: Boolean,
    ) : SyncTransferListItem {
        override val tag get() = row.entry.tag
    }

    /** A completed row. */
    data class Finished(val transfer: SyncCompletedTransfer) : SyncTransferListItem {
        override val tag get() = transfer.entry.tag
    }
}

/**
 * Desktop's tab counters, read from the store's lifetime counts whatever the filter is.
 * [searchUploads] and [searchDownloads] count the search matches per direction and are 0 for
 * every other filter.
 */
internal data class SyncTransferFilterCounts(
    val pending: Int,
    val pendingUploads: Int,
    val pendingDownloads: Int,
    val completed: Int,
    val byMediaType: ImmutableMap<SyncMediaType, Int>,
    val searchUploads: Int,
    val searchDownloads: Int,
) {
    companion object {
        /** Every counter at zero. */
        val Empty: SyncTransferFilterCounts = SyncTransferFilterCounts(
            pending = 0,
            pendingUploads = 0,
            pendingDownloads = 0,
            completed = 0,
            byMediaType = persistentMapOf(),
            searchUploads = 0,
            searchDownloads = 0,
        )
    }
}

/** The rows a filter shows in desktop order, at most the window, with [totalCount] of them all. */
internal data class SyncTransferList(
    val isSeeded: Boolean,
    val items: ImmutableList<SyncTransferListItem>,
    val totalCount: Int,
    val counts: SyncTransferFilterCounts,
)
