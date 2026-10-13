package mega.privacy.android.feature.sync.ui.mapper.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveTransferUiModel
import javax.inject.Inject

/**
 * Maps one pending sync transfer row to [SyncActiveTransferUiModel].
 */
internal class SyncActiveTransferUiModelMapper @Inject constructor() {

    /**
     * Maps one pending row from its [rowState] and its [band]; the quota flags follow the
     * per-direction rule. The ViewModel computes both, so the item's own status and own pause
     * are not read. [isRetryingTransferOverQuota] is true while any live row retries over the
     * transfer quota, which the account blocker can miss.
     */
    operator fun invoke(
        item: SyncTransferListItem.Active,
        rowState: SyncTransferRowState,
        blockers: SyncProgressBlockers,
        band: Int,
        isRetryingTransferOverQuota: Boolean,
    ): SyncActiveTransferUiModel {
        val entry = item.row.entry
        val status = rowState.status
        val isDownload = entry.direction == SyncTransferDirection.Download
        val isTransferOverQuota = status == SyncTransferStatus.OutOfTransferQuota ||
                (isDownload && (blockers.isTransferOverQuota || isRetryingTransferOverQuota))
        // A foreign over quota row keeps its status, but the account itself is not full.
        val isStorageOverQuota =
            (status == SyncTransferStatus.OutOfStorageSpace && !entry.isForeignOverQuota) ||
                    (!isDownload && blockers.isStorageOverQuota)
        return SyncActiveTransferUiModel(
            tag = entry.tag,
            fileName = entry.fileName,
            direction = entry.direction,
            status = status,
            transferredBytes = entry.transferredBytes,
            totalBytes = entry.totalBytes,
            // The domain value is not clamped (desktop parity), but a progress bar rejects
            // values above full.
            progressPermille = rowState.progressPermille.coerceIn(0, FULL_PERMILLE),
            speed = rowState.speed,
            etaSeconds = rowState.etaSeconds,
            isOwnPause = rowState.isOwnPause,
            isStorageOverQuota = isStorageOverQuota,
            isTransferOverQuota = isTransferOverQuota,
            hasIssues = isTransferOverQuota || isStorageOverQuota || blockers.isOffline,
            // The SDK never clears the last error, so a healthy row must not show a stale one.
            lastErrorCode = entry.lastError?.code?.takeIf { status in ERROR_STATUSES },
            isDraggable = status != SyncTransferStatus.Completing,
            band = band,
        )
    }

    private companion object {
        const val FULL_PERMILLE = 1000
        val ERROR_STATUSES = setOf(
            SyncTransferStatus.Retrying,
            SyncTransferStatus.OutOfStorageSpace,
            SyncTransferStatus.OutOfTransferQuota,
        )
    }
}
