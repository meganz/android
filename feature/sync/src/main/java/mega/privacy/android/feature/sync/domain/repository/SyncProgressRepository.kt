package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot

/** The app-wide run snapshot of every sync and the progress of the current run. */
internal interface SyncProgressRepository {

    /** App-scoped; the first value may be NotSeeded. Nothing is collected before the first subscriber. */
    fun monitorSyncRunSnapshot(): Flow<SyncRunSnapshot>

    /** Returns the run snapshot to its initial state and re-reads the sync list; dropped before the first collection. */
    suspend fun resetSyncRunSnapshot()
}
