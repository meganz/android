package mega.privacy.android.feature.sync.ui.mapper.progress

import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveListItem
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressBody
import javax.inject.Inject

/**
 * Maps the status and a pending projection to the body of a pending tab.
 */
internal class SyncProgressBodyMapper @Inject constructor(
    private val activeMapper: SyncActiveTransferUiModelMapper,
) {

    /**
     * Picks a pending tab's body: Loading until the status, the store and the root node are known;
     * rows with the footer. [rowStates] and [bands] are keyed by tag and hold one entry for every
     * active item of [list].
     */
    operator fun invoke(
        status: SyncProgressStatus,
        list: SyncTransferList?,
        blockers: SyncProgressBlockers,
        isRootNodeReady: Boolean,
        rowStates: Map<Int, SyncTransferRowState>,
        bands: Map<Int, Int>,
        isRetryingTransferOverQuota: Boolean,
    ): SyncProgressBody {
        if (!status.isSeeded || list == null || !list.isSeeded || !isRootNodeReady) {
            return SyncProgressBody.Loading
        }
        val rows = list.items.filterIsInstance<SyncTransferListItem.Active>()
        if (rows.isNotEmpty()) {
            return transfersBody(status, rows, blockers, rowStates, bands, isRetryingTransferOverQuota)
        }
        return SyncProgressBody.Loading
    }

    private fun transfersBody(
        status: SyncProgressStatus,
        rows: List<SyncTransferListItem.Active>,
        blockers: SyncProgressBlockers,
        rowStates: Map<Int, SyncTransferRowState>,
        bands: Map<Int, Int>,
        isRetryingTransferOverQuota: Boolean,
    ): SyncProgressBody.Transfers {
        val items = rows.map {
            SyncActiveListItem.Row(
                activeMapper(
                    it,
                    rowStates.getValue(it.tag),
                    blockers,
                    bands.getValue(it.tag),
                    isRetryingTransferOverQuota,
                )
            )
        }
        // The episode count also covers the rows beyond the window and the work that has no
        // row, so the footer agrees with the files left.
        val moreWaiting = (status.progress.pendingItems - rows.size).coerceAtLeast(0)
        val withFooter = if (moreWaiting > 0) items + SyncActiveListItem.MoreWaiting(moreWaiting) else items
        return SyncProgressBody.Transfers(items = withFooter.toImmutableList())
    }
}
