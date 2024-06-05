package mega.privacy.android.app.presentation.fileexplorer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent

/**
 * File explorer view state
 *
 * @property uploadEvent event to trigger upload actions
 */
data class FileExplorerUiState(
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
)