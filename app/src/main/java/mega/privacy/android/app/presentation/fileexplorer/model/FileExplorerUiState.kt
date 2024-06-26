package mega.privacy.android.app.presentation.fileexplorer.model

import android.net.Uri
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent

/**
 * File explorer view state
 *
 * @property uploadEvent event to trigger upload actions
 * @property urisAndNames Map of Uris representing the files to share along with their file names.
 * @property fileNames Map of file names and their corresponding new names if any after renaming.
 */
data class FileExplorerUiState(
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
    val urisAndNames: Map<Uri, String?> = emptyMap(),
    val fileNames: Map<String, String> = emptyMap(),
)