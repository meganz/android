package mega.privacy.android.feature.sync.domain.entity.progress

/** One input of the store's single writer. */
internal sealed interface SyncTransferStoreInput {

    /** An update from the sync transfer feed. */
    data class Feed(val update: SyncTransferUpdate) : SyncTransferStoreInput

    /** Removes the completed rows with these tags. */
    data class ClearCompleted(val tags: Set<Int>) : SyncTransferStoreInput

    /** Removes every completed row, and the current batch's evicted completions. */
    data object ClearAllCompleted : SyncTransferStoreInput

    /** A row pause or resume that the SDK has not confirmed yet. */
    data class SetOptimisticPause(val tags: Set<Int>, val isPaused: Boolean) : SyncTransferStoreInput

    /** Drops a row's optimistic pause after its request failed. */
    data class RevertOptimisticPause(val tag: Int) : SyncTransferStoreInput
}
