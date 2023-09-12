package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest.TYPE_PAUSE_TRANSFER
import javax.inject.Inject

/**
 * [RequestEvent] to [TransferEvent] mapper.
 * It checks if the [RequestEvent] is a Transfer pause finish request and in this case it returns the corresponding [TransferEvent]
 */
internal class PausedTransferEventMapper @Inject constructor() {
    suspend operator fun invoke(
        event: RequestEvent,
        getTransferFromTag: suspend (Int) -> Transfer?,
    ): TransferEvent? = event.takeIf {
        it is RequestEvent.OnRequestFinish
                && it.request.type == TYPE_PAUSE_TRANSFER
                && it.error.errorCode == MegaError.API_OK
    }?.let {
        getTransferFromTag(event.request.transferTag)?.let { transfer ->
            TransferEvent.TransferPaused(transfer, event.request.flag)
        }
    }
}
