package mega.privacy.android.app.presentation.fileexplorer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.document.DocumentEntity
import kotlin.collections.associate

/**
 * File explorer view state
 *
 * @property uploadEvent event to trigger upload actions
 * @property urisAndNames Map of Uris representing the files to share along with their file names.
 * @property fileNames Map of file names and their corresponding new names if any after renaming.
 */
data class FileExplorerUiState(
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
    val documents: List<DocumentEntity> = emptyList(),
){
    /**
     * Documents associated by its uri value
     */
    val documentsByUriPathValue by lazy {
        documents.associateBy { it.uri.value }
    }

    /**
     * File names associated by its uri value
     */
    val namesByUriPathValues by lazy {
            documents.associate { it.uri.value to it.name }
    }

    /**
     * File names associated by its original file name
     */
    val namesByOriginalName by lazy {
        documents.associate { it.originalName to it.name }
    }
}