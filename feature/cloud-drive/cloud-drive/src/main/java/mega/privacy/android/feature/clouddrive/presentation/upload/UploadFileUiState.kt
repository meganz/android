package mega.privacy.android.feature.clouddrive.presentation.upload

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

data class UploadFileUiState(
    val overQuotaEvent: StateEvent = consumed,
    val nameCollisionEvent: StateEventWithContent<List<NameCollision>> = consumed(),
    val startUploadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val uploadErrorEvent: StateEventWithContent<Throwable> = consumed(),
)