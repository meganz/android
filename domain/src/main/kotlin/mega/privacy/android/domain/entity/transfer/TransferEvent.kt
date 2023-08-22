package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.exception.MegaException


/**
 * Transfer event domain model of GlobalTransfer object
 *
 * @property transfer
 */
sealed interface TransferEvent {
    val transfer: Transfer


    /**
     * Transfer start event
     *
     * @property transfer
     */
    data class TransferStartEvent(override val transfer: Transfer) : TransferEvent

    /**
     * Transfer finish event
     *
     * @property transfer
     * @property error
     */
    data class TransferFinishEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent

    /**
     * Transfer update event
     *
     * @property transfer
     */
    data class TransferUpdateEvent(override val transfer: Transfer) :
        TransferEvent

    /**
     * Transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class TransferTemporaryErrorEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent

    /**
     * Transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class TransferDataEvent(override val transfer: Transfer, val buffer: ByteArray?) :
        TransferEvent

    /**
     * Transfer has been paused or resumed
     * @param paused
     */
    data class TransferPaused(override val transfer: Transfer, val paused: Boolean) : TransferEvent
}
