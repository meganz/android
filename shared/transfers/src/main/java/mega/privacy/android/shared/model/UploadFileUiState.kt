package mega.privacy.android.shared.transfers.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

data class UploadFileUiState(
    /**
     * True from the moment an upload request is accepted for processing (file
     * preparation, collision checks) until it fails; stays true once the upload
     * has been handed over, as the host screen closes at that point.
     */
    val isProcessing: Boolean = false,
    val overQuotaEvent: StateEvent = consumed,
    val nameCollisionEvent: StateEventWithContent<List<NameCollision>> = consumed(),
    val startUploadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val uploadErrorEvent: StateEventWithContent<Throwable> = consumed(),
)

