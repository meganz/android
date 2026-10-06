package mega.privacy.android.feature.sync.data.mapper.progress

import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import nz.mega.sdk.MegaSync
import javax.inject.Inject

/**
 * Maps a sync to its work state. Stats and the running since time are kept only while Running,
 * because the SDK rebuilds its counters whenever a sync leaves Running.
 */
internal class SyncWorkStateMapper @Inject constructor(
    private val syncRunStateMapper: SyncRunStateMapper,
    private val syncErrorMapper: SyncErrorMapper,
) {

    operator fun invoke(
        sync: MegaSync,
        stats: SyncStatsSample?,
        runningSinceMillis: Long?,
    ): SyncWorkState {
        val runState = syncRunStateMapper(sync.runState)
        val syncError = syncErrorMapper(sync.error)
        val isRunning = runState == SyncRunState.Running
        return SyncWorkState(
            syncId = sync.backupId,
            runState = runState,
            isStorageOverQuota = syncError == SyncError.STORAGE_OVERQUOTA,
            stats = if (isRunning) stats else null,
            runningSinceMillis = if (isRunning) runningSinceMillis else null,
            hasError = syncError != SyncError.NO_SYNC_ERROR,
        )
    }
}
