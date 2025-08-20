package mega.privacy.android.app.appstate.transfer

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

data class AppTransferUiState(
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)