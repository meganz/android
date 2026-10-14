package mega.privacy.android.feature.sync.domain.entity.progress

/** A reorder request for one sync row; [Drop] carries the tags as shown after a drag. */
internal sealed interface SyncTransferMove {

    /** Moves the row to the first position of its direction's queue. */
    data object ToTop : SyncTransferMove

    /** Moves the row one visible step up in its band. */
    data object Up : SyncTransferMove

    /** Moves the row one visible step down in its band. */
    data object Down : SyncTransferMove

    /** Moves the row to the last position of its direction's queue. */
    data object ToBottom : SyncTransferMove

    /** Moves the row to where a drag dropped it; [displayedOrder] holds the tags as shown. */
    data class Drop(val displayedOrder: List<Int>) : SyncTransferMove
}
