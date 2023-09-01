package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync.SyncRunningState
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

/**
 * This class handles mapping Sync Status based on SyncStats and RunningState.
 *
 * Currently the mapping to Error state is not implemented. It will be implemented with Stall Issues
 */
internal class SyncStatusMapper @Inject constructor() {

    operator fun invoke(
        syncStats: MegaSyncStats?,
        runningState: Int,
    ): SyncStatus = when {
        runningState == SyncRunningState.RUNSTATE_SUSPENDED.swigValue() -> SyncStatus.PAUSED
        syncStats == null -> SyncStatus.SYNCED
        syncStats.isScanning || syncStats.isSyncing -> SyncStatus.SYNCING
        else -> SyncStatus.SYNCED
    }
}
