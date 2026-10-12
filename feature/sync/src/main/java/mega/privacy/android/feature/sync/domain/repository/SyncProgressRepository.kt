package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkStatesUpdate

/** The app-wide run snapshot of every sync and the progress of the current run. */
internal interface SyncProgressRepository {

    /** The published run snapshot, the same flow for every caller; NotSeeded until the run seeds it. */
    fun monitorSyncRunSnapshot(): Flow<SyncRunSnapshot>

    /**
     * The syncs after each applied sync event of the current generation, once the sync list is read.
     * Cold, for one collector: collecting it subscribes to the sync listener.
     */
    fun monitorSyncWorkStates(): Flow<SyncWorkStatesUpdate>

    /** Publishes the folded run snapshot; dropped unless [generation] is still the current one. */
    suspend fun publishSyncRunSnapshot(snapshot: SyncRunSnapshot, generation: Long)

    /** Starts a new generation and publishes NotSeeded at once; the sync list is read again. */
    suspend fun resetSyncRunSnapshot()
}
