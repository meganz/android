package mega.privacy.android.app.presentation.upload

import android.net.Uri
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * UI state for [UploadDestinationActivity]
 *
 * @property isNewUploadScreenEnabled True if the new upload activity is enabled, false otherwise
 * @property fileUriList List of [Uri] of the files to upload
 * @property importUiItems List of [ImportUiItem] to show the files to import
 * @property nameValidationError Event to show the name validation error
 * @property navigateToUpload Event to navigate to the upload screen
 * @property editableFile The file name to edit
 */
data class UploadDestinationUiState(
    val isNewUploadScreenEnabled: Boolean? = null,
    val fileUriList: List<Uri> = emptyList(),
    val importUiItems: List<ImportUiItem> = emptyList(),
    val nameValidationError: StateEventWithContent<String> = consumed(),
    val navigateToUpload: StateEventWithContent<List<Uri>> = consumed(),
    val editableFile: ImportUiItem? = null
)
