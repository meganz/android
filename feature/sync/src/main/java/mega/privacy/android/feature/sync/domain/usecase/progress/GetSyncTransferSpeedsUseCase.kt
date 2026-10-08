package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import javax.inject.Inject

/**
 * Returns the upload and download speeds of sync transfers. Each direction is null while none of
 * its sync transfers is pending, else the smaller of its ACTIVE rows' summed speed and the SDK's
 * direction-wide speed, which also counts non-sync transfers. A null SDK speed is not known, so
 * the row sum is used.
 */
internal class GetSyncTransferSpeedsUseCase @Inject constructor() {

    operator fun invoke(
        state: SyncTransferStoreState,
        sdkUploadSpeed: Long?,
        sdkDownloadSpeed: Long?,
    ): SyncTransferSpeeds {
        var uploads = 0L
        var downloads = 0L
        state.active.values.forEach { row ->
            if (row.entry.state == TransferState.STATE_ACTIVE) {
                when (row.entry.direction) {
                    SyncTransferDirection.Upload -> uploads += row.entry.speed
                    SyncTransferDirection.Download -> downloads += row.entry.speed
                }
            }
        }
        return SyncTransferSpeeds(
            uploadSpeed = directionSpeed(
                state.counts.lifetimeUploads.pending,
                uploads,
                sdkUploadSpeed,
            ),
            downloadSpeed = directionSpeed(
                state.counts.lifetimeDownloads.pending,
                downloads,
                sdkDownloadSpeed,
            ),
        )
    }

    private fun directionSpeed(pending: Int, rowSum: Long, sdkSpeed: Long?): Long? =
        if (pending > 0) minOf(rowSum, sdkSpeed ?: rowSum) else null
}
