package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferState
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * map [TransferState] to from SDK int values
 */
internal class TransferStateIntMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param transferState [TransferState]
     */
    operator fun invoke(transferState: TransferState) = when (transferState) {
        TransferState.STATE_NONE -> MegaTransfer.STATE_NONE
        TransferState.STATE_QUEUED -> MegaTransfer.STATE_QUEUED
        TransferState.STATE_ACTIVE -> MegaTransfer.STATE_ACTIVE
        TransferState.STATE_PAUSED -> MegaTransfer.STATE_PAUSED
        TransferState.STATE_RETRYING -> MegaTransfer.STATE_RETRYING
        TransferState.STATE_COMPLETING -> MegaTransfer.STATE_COMPLETING
        TransferState.STATE_COMPLETED -> MegaTransfer.STATE_COMPLETED
        TransferState.STATE_CANCELLED -> MegaTransfer.STATE_CANCELLED
        TransferState.STATE_FAILED -> MegaTransfer.STATE_FAILED
    }
}
