package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import javax.inject.Inject

/**
 * Returns the SDK call for a reorder of one sync row, or null when the move changes nothing.
 * Up, Down and Drop resolve against the same-direction movable rows of the row's band, so a
 * row lands where the user sees it go, although hidden non-sync transfers share the SDK queue.
 */
internal class ResolveSyncTransferMoveUseCase @Inject constructor() {

    /** Picks the SDK call for [move] on [tag]. [pending] is the full Pending list, in its order. */
    operator fun invoke(
        tag: Int,
        move: SyncTransferMove,
        pending: List<SyncTransferListItem>,
    ): ResolvedMove? {
        val moved = pending.movable(tag) ?: return null
        val peers = pending.peersOf(moved)
        val index = peers.indexOf(moved)
        val line = pending.priorityLineOf(moved)
        return when (move) {
            SyncTransferMove.ToTop -> ResolvedMove.ToFirst
            SyncTransferMove.ToBottom -> ResolvedMove.ToLast
            SyncTransferMove.Up -> peers.getOrNull(index - 1)
                ?.let { ResolvedMove.Before(it.tag) }
                ?.unlessNoOp(moved, line)

            SyncTransferMove.Down -> when {
                index == peers.lastIndex -> null
                index + 2 <= peers.lastIndex -> ResolvedMove.Before(peers[index + 2].tag)
                else -> line.after(peers[index + 1], excluding = moved)
            }?.unlessNoOp(moved, line)

            is SyncTransferMove.Drop -> drop(moved, peers, line, move.displayedOrder)
                ?.unlessNoOp(moved, line)
        }
    }

    /** The SDK request for a move. */
    sealed interface ResolvedMove {

        /** Moves the row to the first position of its direction's queue. */
        data object ToFirst : ResolvedMove

        /** Moves the row to the last position of its direction's queue. */
        data object ToLast : ResolvedMove

        /** Moves the row immediately before [prevTag], the row that then follows it. */
        data class Before(val prevTag: Int) : ResolvedMove
    }

    private fun List<SyncTransferListItem>.movable(tag: Int): SyncTransferListItem.Active? =
        filterIsInstance<SyncTransferListItem.Active>()
            .firstOrNull { it.tag == tag }
            ?.takeIf { it.isMovable() }

    private fun List<SyncTransferListItem>.priorityLineOf(
        moved: SyncTransferListItem.Active,
    ): List<SyncTransferListItem.Active> =
        filterIsInstance<SyncTransferListItem.Active>()
            .filter { it.isMovable() && it.row.entry.direction == moved.row.entry.direction }
            .sortedWith(compareBy({ it.row.entry.priority }, { it.tag }))

    private fun drop(
        moved: SyncTransferListItem.Active,
        peers: List<SyncTransferListItem.Active>,
        line: List<SyncTransferListItem.Active>,
        displayedOrder: List<Int>,
    ): ResolvedMove? {
        val position = displayedOrder.withIndex().associate { (index, tag) -> tag to index }
        if (moved.tag !in position) return null
        val shown = peers.filter { it.tag in position }.sortedBy { position.getValue(it.tag) }
        val index = shown.indexOf(moved)
        shown.getOrNull(index + 1)?.let { return ResolvedMove.Before(it.tag) }
        val previous = shown.getOrNull(index - 1) ?: return null
        return line.after(previous, excluding = moved)
    }

    /**
     * Returns a move before the row that follows [anchor] in priority order, or to the last
     * position when none follows. The next row comes from the priority line and not from the
     * band, because rows of other bands and hidden transfers interleave with the band in the SDK
     * queue, so only the line's next row puts the moved row directly after [anchor].
     */
    private fun List<SyncTransferListItem.Active>.after(
        anchor: SyncTransferListItem.Active,
        excluding: SyncTransferListItem.Active,
    ): ResolvedMove {
        val withoutMoved = filterNot { it.tag == excluding.tag }
        val next = withoutMoved.getOrNull(withoutMoved.indexOfFirst { it.tag == anchor.tag } + 1)
        return next?.let { ResolvedMove.Before(it.tag) } ?: ResolvedMove.ToLast
    }

    private fun ResolvedMove.unlessNoOp(
        moved: SyncTransferListItem.Active,
        line: List<SyncTransferListItem.Active>,
    ): ResolvedMove? {
        val movedIndex = line.indexOf(moved)
        val isNoOp = when (this) {
            is ResolvedMove.Before ->
                prevTag == moved.tag || line.getOrNull(movedIndex + 1)?.tag == prevTag

            ResolvedMove.ToLast -> movedIndex == line.lastIndex
            ResolvedMove.ToFirst -> false
        }
        return if (isNoOp) null else this
    }
}
