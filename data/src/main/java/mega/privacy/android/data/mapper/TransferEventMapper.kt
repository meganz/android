package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.TransferEvent

/**
 * [GlobalTransfer] to [TransferEvent] mapper
 */
typealias TransferEventMapper = (@JvmSuppressWildcards GlobalTransfer) -> @JvmSuppressWildcards TransferEvent

internal fun toTransferEventModel(
    event: GlobalTransfer,
    transferMapper: MegaTransferMapper,
    exceptionMapper: MegaExceptionMapper,
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