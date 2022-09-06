package mega.privacy.android.app.data.model

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * Global transfer
 *
 * @property transfer
 */
sealed class GlobalTransfer(open val transfer: MegaTransfer) {
    /**
     * On transfer start
     *
     * @property transfer
     */
    data class OnTransferStart(override val transfer: MegaTransfer) : GlobalTransfer(transfer)

    /**
     * On transfer finish
     *
     * @property transfer
     * @property error
     */
    data class OnTransferFinish(override val transfer: MegaTransfer, val error: MegaError) :
        GlobalTransfer(transfer)

    /**
     * On transfer update
     *
     * @property transfer
     */
    data class OnTransferUpdate(override val transfer: MegaTransfer) : GlobalTransfer(transfer)

    /**
     * On transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class OnTransferTemporaryError(
        override val transfer: MegaTransfer,
        val error: MegaError,
    ) : GlobalTransfer(transfer)

    /**
     * On transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class OnTransferData(override val transfer: MegaTransfer, val buffer: ByteArray?) :
        GlobalTransfer(transfer)
}