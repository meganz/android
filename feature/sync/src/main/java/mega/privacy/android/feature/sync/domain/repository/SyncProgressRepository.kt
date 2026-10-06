package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity

/** The app-wide activity of every sync and the progress of the current run. */
internal interface SyncProgressRepository {

    /** App-scoped; the first value may be NotSeeded. Nothing is collected before the first subscriber. */
    fun monitorSyncActivity(): Flow<SyncActivity>

    /** Returns the activity to its initial state and re-reads the sync list; dropped before the first collection. */
    suspend fun resetSyncActivity()
}
