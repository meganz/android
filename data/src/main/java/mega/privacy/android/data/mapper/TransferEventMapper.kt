package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import javax.inject.Inject

/**
 * [GlobalTransfer] to [TransferEvent] mapper
 */
internal class TransferEventMapper @Inject constructor(
    private val transferMapper: TransferMapper,
    private val exceptionMapper: MegaExceptionMapper,
) {
    operator fun invoke(
        event: GlobalTransfer,
    ) = when (event) {
        is GlobalTransfer.OnTransferData -> TransferEvent.TransferDataEvent(
            transferMapper(event.transfer),
            event.buffer
        )
        is GlobalTransfer.OnTransferFinish -> TransferEvent.TransferFinishEvent(
            transferMapper(event.transfer),
            exceptionMapper(event.error)
        )
        is GlobalTransfer.OnTransferStart -> TransferEvent.TransferStartEvent(
            transferMapper(event.transfer)
        )
        is GlobalTransfer.OnTransferTemporaryError -> TransferEvent.TransferTemporaryErrorEvent(
            transferMapper(event.transfer),
            exceptionMapper(event.error)
        )
        is GlobalTransfer.OnTransferUpdate -> TransferEvent.TransferUpdateEvent(
            transferMapper(event.transfer)
        )
    }
}