package mega.privacy.android.app.appstate.transfer

import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.TransferHandler
import javax.inject.Inject

/**
 * Implementation of TransferHandler that delegates to AppTransferViewModel.
 */
class TransferHandlerImpl @Inject constructor(
    private val appTransferViewModel: AppTransferViewModel,
) : TransferHandler {

    override fun setTransferEvent(event: TransferTriggerEvent) {
        appTransferViewModel.setTransferEvent(event)
    }
} 