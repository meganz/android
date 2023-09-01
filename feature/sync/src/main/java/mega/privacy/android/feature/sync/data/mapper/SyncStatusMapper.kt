package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

internal class SyncStatusMapper @Inject constructor() {

    operator fun invoke(syncStats: MegaSyncStats?): SyncStatus = when {
        syncStats == null -> SyncStatus.MONITORING
        syncStats.isScanning || syncStats.isSyncing -> SyncStatus.SYNCING
        else -> SyncStatus.MONITORING
    }
}
