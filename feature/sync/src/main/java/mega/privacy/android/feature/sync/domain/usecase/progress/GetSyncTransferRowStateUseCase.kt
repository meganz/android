package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import javax.inject.Inject

/** The SDK's API_EOVERQUOTA; the domain layer does not import the SDK. */
internal const val SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE = -17

/**
 * Returns what a sync transfer row shows: its status, own pause, speed, progress and time left.
 * [directionSpeed] caps the row speed; null means it is not known, so the row speed is used.
 */
internal class GetSyncTransferRowStateUseCase @Inject constructor() {

    operator fun invoke(
        row: SyncTransferRow,
        context: SyncTransferRowContext,
        directionSpeed: Long?,
    ): SyncTransferRowState {
        val status = effectiveStatus(row, context)
        val speed = displaySpeed(row.entry, status, directionSpeed)
        return SyncTransferRowState(
            status = status,
            isOwnPause = isOwnPause(row),
            speed = speed,
            progressPermille = progressPermille(row.entry, status),
            etaSeconds = etaSeconds(row.entry, status, speed),
        )
    }

    private fun effectiveStatus(
        row: SyncTransferRow,
        context: SyncTransferRowContext,
    ): SyncTransferStatus {
        val entry = row.entry
        val state = entry.state
        return when {
            state == TransferState.STATE_COMPLETED -> SyncTransferStatus.Completed
            state == TransferState.STATE_COMPLETING -> SyncTransferStatus.Completing
            isShownPaused(row, context) ->
                if (entry.transferredBytes > 0) SyncTransferStatus.Paused
                else SyncTransferStatus.PausedInQueue

            else -> quotaStatus(entry, context) ?: when {
                state == TransferState.STATE_RETRYING -> SyncTransferStatus.Retrying
                row.optimisticPause == false
                        || state == TransferState.STATE_QUEUED
                        || state == TransferState.STATE_NONE -> SyncTransferStatus.Queued

                state == TransferState.STATE_ACTIVE && entry.transferredBytes == 0L ->
                    SyncTransferStatus.Starting

                state == TransferState.STATE_ACTIVE -> SyncTransferStatus.Transferring
                else -> SyncTransferStatus.Queued
            }
        }
    }

    private fun isOwnPause(row: SyncTransferRow): Boolean =
        row.optimisticPause ?: (row.entry.state == TransferState.STATE_PAUSED)

    private fun isShownPaused(row: SyncTransferRow, context: SyncTransferRowContext): Boolean =
        row.optimisticPause ?: (row.entry.state == TransferState.STATE_PAUSED
                || (context.areTransfersPaused && isSyncPausable(row.entry.state)))

    private fun isSyncPausable(state: TransferState): Boolean =
        state == TransferState.STATE_QUEUED
                || state == TransferState.STATE_ACTIVE
                || state == TransferState.STATE_RETRYING

    private fun quotaStatus(
        entry: SyncTransferEntry,
        context: SyncTransferRowContext,
    ): SyncTransferStatus? {
        val error = entry.lastError?.takeIf { it.code == SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE }
            ?: return null
        val isTransferQuota = error.value != 0L
        val status = if (isTransferQuota) {
            SyncTransferStatus.OutOfTransferQuota
        } else {
            SyncTransferStatus.OutOfStorageSpace
        }
        return when (entry.state) {
            TransferState.STATE_RETRYING -> status
            // The SDK never clears lastError, so a queued row shows the label only while the
            // account still matches it.
            TransferState.STATE_QUEUED -> status.takeIf {
                if (isTransferQuota) context.isTransferOverQuota
                else context.isStorageOverQuota || entry.isForeignOverQuota
            }

            else -> null
        }
    }

    private fun displaySpeed(
        entry: SyncTransferEntry,
        status: SyncTransferStatus,
        directionSpeed: Long?,
    ): Long = when (status) {
        SyncTransferStatus.Completing -> 0L
        SyncTransferStatus.Completed ->
            if (entry.meanSpeed != 0L) entry.meanSpeed else entry.totalBytes

        else -> minOf(entry.speed, directionSpeed ?: entry.speed)
    }

    private fun progressPermille(entry: SyncTransferEntry, status: SyncTransferStatus): Int =
        when {
            status == SyncTransferStatus.Completed
                    || status == SyncTransferStatus.Completing -> PERMILLE

            entry.totalBytes <= 0 -> 0
            else -> (entry.transferredBytes * PERMILLE / entry.totalBytes).toInt()
        }

    private fun etaSeconds(
        entry: SyncTransferEntry,
        status: SyncTransferStatus,
        speed: Long,
    ): Long? =
        if (status == SyncTransferStatus.Transferring
            && speed > 0
            && entry.totalBytes > entry.transferredBytes
        ) {
            (entry.totalBytes - entry.transferredBytes) / speed
        } else {
            null
        }

    private companion object {
        const val PERMILLE = 1000
    }
}
