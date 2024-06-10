package mega.privacy.android.app.uploadFolder

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent

/**
 * View state for the upload folder screen
 * @param transferTriggerEvent: the event to start the download
 */
data class UploadFolderViewState(
    val transferTriggerEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
)