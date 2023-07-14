package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.exception.MegaException


/**
 * Transfer event domain model of GlobalTransfer object
 *
 * @property transfer
 */
sealed class TransferEvent(open val transfer: Transfer) {

    /**
     * Transfer start event
     *
     * @property transfer
     */
    data class TransferStartEvent(override val transfer: Transfer) :
        TransferEvent(transfer)

    /**
     * Transfer finish event
     *
     * @property transfer
     * @property error
     */
    data class TransferFinishEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent(transfer)

    /**
     * Transfer update event
     *
     * @property transfer
     */
    data class TransferUpdateEvent(override val transfer: Transfer) :
        TransferEvent(transfer)

    /**
     * Transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class TransferTemporaryErrorEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent(transfer)

    /**
     * Transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class TransferDataEvent(override val transfer: Transfer, val buffer: ByteArray?) :
        TransferEvent(transfer)
}
