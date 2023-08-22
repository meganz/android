package mega.privacy.android.data.model

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * Global transfer
 *
 * @property transfer
 */
sealed interface GlobalTransfer {
    val transfer: MegaTransfer

    /**
     * On transfer start
     *
     * @property transfer
     */
    data class OnTransferStart(override val transfer: MegaTransfer) : GlobalTransfer

    /**
     * On transfer finish
     *
     * @property transfer
     * @property error
     */
    data class OnTransferFinish(override val transfer: MegaTransfer, val error: MegaError) :
        GlobalTransfer

    /**
     * On transfer update
     *
     * @property transfer
     */
    data class OnTransferUpdate(override val transfer: MegaTransfer) : GlobalTransfer

    /**
     * On transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class OnTransferTemporaryError(
        override val transfer: MegaTransfer,
        val error: MegaError,
    ) : GlobalTransfer

    /**
     * On transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class OnTransferData(override val transfer: MegaTransfer, val buffer: ByteArray?) :
        GlobalTransfer
}