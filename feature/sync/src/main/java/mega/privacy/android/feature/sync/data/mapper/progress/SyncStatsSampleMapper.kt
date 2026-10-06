package mega.privacy.android.feature.sync.data.mapper.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

/** Maps one SDK stats sample of a sync. */
internal class SyncStatsSampleMapper @Inject constructor() {

    operator fun invoke(stats: MegaSyncStats): SyncStatsSample = SyncStatsSample(
        isScanning = stats.isScanning,
        isSyncing = stats.isSyncing,
        pendingUploads = stats.uploadCount,
        pendingDownloads = stats.downloadCount,
    )
}
