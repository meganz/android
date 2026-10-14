package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import javax.inject.Inject

/**
 * Returns the moves desktop offers at a row's position in the displayed list. Up and Down also
 * need a same-direction movable row of the same band, because a step without one changes
 * nothing that the user can see.
 */
internal class GetAvailableSyncTransferMovesUseCase @Inject constructor() {

    /**
     * Returns the moves for [tag]. [displayed] is the listed window of a list with [totalCount]
     * rows, so the last row of a full window still offers [SyncTransferMove.ToBottom].
     */
    operator fun invoke(
        tag: Int,
        displayed: List<SyncTransferListItem>,
        totalCount: Int = displayed.size,
    ): Set<SyncTransferMove> {
        val position = displayed.indexOfFirst { it.tag == tag }
        if (position < 0 || totalCount < 2) return emptySet()
        val moved = displayed[position] as? SyncTransferListItem.Active ?: return emptySet()
        if (!moved.isMovable()) return emptySet()
        val peers = displayed.peersOf(moved)
        val index = peers.indexOf(moved)
        return buildSet {
            if (position > 0) add(SyncTransferMove.ToTop)
            if (index > 0) add(SyncTransferMove.Up)
            if (index < peers.lastIndex) add(SyncTransferMove.Down)
            if (position < totalCount - 1) add(SyncTransferMove.ToBottom)
        }
    }
}

/** True when the SDK can move the row or place another row before it. */
internal fun SyncTransferListItem.Active.isMovable(): Boolean = row.entry.state in MOVABLE_STATES

/** The movable rows of the same direction and band as [moved], in list order, [moved] included. */
internal fun List<SyncTransferListItem>.peersOf(
    moved: SyncTransferListItem.Active,
): List<SyncTransferListItem.Active> =
    filterIsInstance<SyncTransferListItem.Active>().filter {
        it.isMovable()
                && it.row.entry.direction == moved.row.entry.direction
                && it.band == moved.band
    }

private val MOVABLE_STATES = setOf(
    TransferState.STATE_QUEUED,
    TransferState.STATE_ACTIVE,
    TransferState.STATE_RETRYING,
    TransferState.STATE_PAUSED,
)
