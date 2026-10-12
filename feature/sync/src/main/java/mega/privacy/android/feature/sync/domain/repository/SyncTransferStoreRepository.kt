package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreCommand
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState

/** The always-on store of sync transfers: its published state, and the commands its run folds. */
internal interface SyncTransferStoreRepository {

    /** The published store state, the same flow for every caller; NotSeeded until the run seeds it. */
    fun monitorSyncTransferStore(): Flow<SyncTransferStoreState>

    /**
     * Feed updates and commands of the current generation, in arrival order. Cold, for one
     * collector: collecting it starts the sync transfer feed, cancelling it stops the feed.
     */
    fun monitorSyncTransferStoreCommands(): Flow<SyncTransferStoreCommand>

    /** Publishes the folded state; dropped unless [generation] is still the current one. */
    suspend fun publishSyncTransferStore(state: SyncTransferStoreState, generation: Long)

    /** Removes the completed rows with these tags. */
    suspend fun clearCompleted(tags: Set<Int>)

    /** Removes every completed row. */
    suspend fun clearAllCompleted()

    /** Marks these rows paused or resumed until the SDK confirms it. */
    suspend fun setOptimisticPause(tags: Set<Int>, isPaused: Boolean)

    /** Drops a row's optimistic pause after its request failed. */
    suspend fun revertOptimisticPause(tag: Int)

    /** Reconciles the active rows with a fresh snapshot, through the ordered feed. */
    suspend fun requestReseed()

    /** Starts a new generation and publishes NotSeeded at once; the feed is read again. Never starts it. */
    suspend fun reset()
}
