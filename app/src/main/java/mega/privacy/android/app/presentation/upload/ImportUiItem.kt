package mega.privacy.android.app.presentation.upload

/**
 * UI item for import files
 *
 * @property filePath Path of the file
 * @property originalFileName Name of the file
 * @property fileIcon Icon of the file
 * @property error Error message for the file name if any
 * @property fileIcon Icon of the file
 * @property isUrl True if the file is an URL, false otherwise
 */
data class ImportUiItem(
    val filePath: String?,
    val originalFileName: String,
    val fileName: String,
    val fileIcon: Int? = null,
    val error: String? = null,
    val isUrl: Boolean = false,
)
