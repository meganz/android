package mega.privacy.android.app.presentation.upload

/**
 * UI item for import files
 *
 * @property filePath Path of the file
 * @property fileName Name of the file
 * @property error Error message for the file name if any
 */
data class ImportUiItem(
    val filePath: String,
    val fileName: String,
    val error: String? = null
)
