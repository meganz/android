package mega.privacy.android.feature.sync.domain.entity.progress

/** The pure sync transfer store: one input in, one immutable state out, within these bounds. */
internal object SyncTransferStore {

    /** At or above this many tracked rows, a tag is admitted or promoted only in a slot-holding state. */
    const val TRACKED_ROW_LIMIT = 20_000

    /** How far slot-holding transfers may take the tracked rows above [TRACKED_ROW_LIMIT]. */
    const val SLOT_HOLDER_HEADROOM = 256

    /** Completed rows kept, newest first; each older one is evicted. */
    const val COMPLETED_ROW_LIMIT = 500

    /** The state of a new generation. */
    fun reset(): SyncTransferStoreState = SyncTransferStoreState.NotSeeded
}
