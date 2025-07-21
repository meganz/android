package mega.privacy.android.app.main.managerSections

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.transfer.Transfer

/**
 * Transfer ui state
 *
 * @property pauseOrResumeTransferResult
 * @property cancelTransfersResult
 * @property startEvent event to start a new transfer
 * @property isInTransferOverQuota
 * @property readRetryError Null if there is no error, read retry error count otherwise
 */
data class TransfersUiState(
    val pauseOrResumeTransferResult: Result<Transfer>? = null,
    val cancelTransfersResult: Result<Unit>? = null,
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val isInTransferOverQuota: Boolean = false,
    val readRetryError: Int? = null,
)