package mega.privacy.android.app.presentation.upload

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * UI state for [UploadDestinationActivity]
 *
 * @property isNewUploadScreenEnabled True if the new upload activity is enabled, false otherwise
 * @property importUiItems List of [ImportUiItem] to show the files to import
 * @property nameValidationError Event to show the name validation error
 * @property editableFile The file name to edit
 */
data class UploadDestinationUiState(
    val isNewUploadScreenEnabled: Boolean? = null,
    val importUiItems: List<ImportUiItem> = emptyList(),
    val nameValidationError: StateEventWithContent<String> = consumed(),
    val editableFile: ImportUiItem? = null,
) {
    val nameMap = hashMapOf<String, String>().apply {
        importUiItems.forEach {
            put(it.originalFileName, it.fileName)
        }
    }
}
