package mega.privacy.android.app.main.managerSections

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.transfer.Transfer

/**
 * Transfer ui state
 *
 * @property pauseOrResumeTransferResult
 * @property cancelTransfersResult
 * @property startEvent event to start a new transfer
 */
data class TransfersUiState(
    val pauseOrResumeTransferResult: Result<Transfer>? = null,
    val cancelTransfersResult: Result<Unit>? = null,
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)