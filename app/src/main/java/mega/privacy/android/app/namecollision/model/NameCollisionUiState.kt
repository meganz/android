package mega.privacy.android.app.namecollision.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Ui State for Name Collision.
 *
 * @property uploadEvent Event to trigger upload actions.
 */
data class NameCollisionUiState(
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload.CollidedFiles> = consumed(),
)
