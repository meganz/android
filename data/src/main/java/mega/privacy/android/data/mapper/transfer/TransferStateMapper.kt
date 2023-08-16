package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferState
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [TransferState] mapper from SDK int values
 */
internal class TransferStateMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param transferStateInt [Int]
     */
    operator fun invoke(transferStateInt: Int) = when (transferStateInt) {
        MegaTransfer.STATE_NONE -> TransferState.STATE_NONE
        MegaTransfer.STATE_QUEUED -> TransferState.STATE_QUEUED
        MegaTransfer.STATE_ACTIVE -> TransferState.STATE_ACTIVE
        MegaTransfer.STATE_PAUSED -> TransferState.STATE_PAUSED
        MegaTransfer.STATE_RETRYING -> TransferState.STATE_RETRYING
        MegaTransfer.STATE_COMPLETING -> TransferState.STATE_COMPLETING
        MegaTransfer.STATE_COMPLETED -> TransferState.STATE_COMPLETED
        MegaTransfer.STATE_CANCELLED -> TransferState.STATE_CANCELLED
        MegaTransfer.STATE_FAILED -> TransferState.STATE_FAILED
        else -> TransferState.STATE_NONE
    }
}
