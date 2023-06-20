package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException

/**
 * Events for transfers that involves more than one node
 */
sealed interface MultiTransferEvent

/**
 * Transfer for this node has not been started for some reason
 * @param nodeId [NodeId] of the node that has not been transferred
 * @param exception that caused the issue
 */
data class TransferNotStarted(val nodeId: NodeId, val exception: Throwable?) :
    MultiTransferEvent

/**
 * All transfers has been processed by the sdk, starting from this event transfers can be retried by sdk if the app is closed
 */
object FinishProcessingTransfers : MultiTransferEvent


/**
 * Transfer event domain model of GlobalTransfer object
 *
 * @property transfer
 */
sealed class TransferEvent(open val transfer: Transfer) : MultiTransferEvent {

    internal fun isFinishProcessingEvent() = when {
        this is TransferUpdateEvent &&
                transfer.isFolderTransfer && transfer.stage == TransferStage.STAGE_TRANSFERRING_FILES -> {
            true
        }

        this is TransferFinishEvent &&
                transfer.isFolderTransfer -> {
            true
        }

        this is TransferStartEvent &&
                !transfer.isFolderTransfer -> {
            true
        }

        else -> false
    }

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
        val error: MegaException,
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
        val error: MegaException,
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