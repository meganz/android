package mega.privacy.android.app.presentation.upload

import android.net.Uri

/**
 * UI state for [UploadDestinationActivity]
 *
 * @property isNewUploadScreenEnabled True if the new upload activity is enabled, false otherwise
 * @property fileUriList List of [Uri] of the files to upload
 * @property importUiItems List of [ImportUiItem] to show the files to import
 */
data class UploadDestinationUiState(
    val isNewUploadScreenEnabled: Boolean? = null,
    val fileUriList: List<Uri> = emptyList(),
    val importUiItems: List<ImportUiItem> = emptyList(),
)
