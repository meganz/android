package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import nz.mega.sdk.MegaError
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

        is GlobalTransfer.OnTransferFinish -> {
            val exception = when (event.error.errorCode) {
                MegaError.API_OK -> null
                else -> exceptionMapper(event.error)
            }
            TransferEvent.TransferFinishEvent(
                transferMapper(event.transfer),
                exception
            )
        }

        is GlobalTransfer.OnTransferStart -> TransferEvent.TransferStartEvent(
            transferMapper(event.transfer)
        )

        is GlobalTransfer.OnTransferTemporaryError -> {
            val exception = when (event.error.errorCode) {
                MegaError.API_OK -> null
                else -> exceptionMapper(event.error)
            }
            TransferEvent.TransferTemporaryErrorEvent(
                transferMapper(event.transfer),
                exception
            )
        }

        is GlobalTransfer.OnTransferUpdate -> TransferEvent.TransferUpdateEvent(
            transferMapper(event.transfer)
        )
    }
}
