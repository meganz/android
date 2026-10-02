package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState

/** The always-on store of sync transfers: commands for its single writer, and its published state. */
internal interface SyncTransferStoreRepository {

    /** App-scoped; the first value may be NotSeeded. Nothing is collected before the first subscriber. */
    fun monitorSyncTransferStore(): Flow<SyncTransferStoreState>

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

    /** Starts a new generation: the store returns to NotSeeded and re-reads the feed. Never starts the store. */
    suspend fun reset()
}
