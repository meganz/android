package mega.privacy.android.app.presentation.transfers.model.image

import android.net.Uri

/**
 * UI state for Transfer items.
 *
 * @property fileTypeResId Resource ID of the file type icon.
 * @property previewUri Uri of the preview image.
 */
data class TransferImageUiState(
    val fileTypeResId: Int? = null,
    val previewUri: Uri? = null,
)