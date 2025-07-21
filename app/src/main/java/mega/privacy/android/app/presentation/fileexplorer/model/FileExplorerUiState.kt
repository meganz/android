package mega.privacy.android.app.presentation.fileexplorer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * The File Explorer UI State
 *
 * @property uploadEvent event to trigger upload actions
 * @property documents the list of documents
 * @property hasMultipleScans true if there is more than one scan to be uploaded
 * @property isUploadingScans true if there are scans to be uploaded
 * @property isScanUploadingAborted true if the User is in the process of uploading the scans, but
 * decides to back out of the process
 * @property shouldFinishScreen true if the File Explorer should be finished
 */
data class FileExplorerUiState(
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
    val documents: List<DocumentEntity> = emptyList(),
    val hasMultipleScans: Boolean = false,
    val isUploadingScans: Boolean = false,
    val isScanUploadingAborted: Boolean = false,
    val shouldFinishScreen: Boolean = false,
) {
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