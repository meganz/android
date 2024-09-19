package mega.privacy.android.app.presentation.documentscanner.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Enum class holding different Snackbar messages for the Scan Confirmation page
 *
 * @property textRes A StringRes resource for the error message
 * @property formatArgsText A String for the [textRes] having format arguments
 */
enum class SaveScannedDocumentsSnackbarMessageUiItem(
    @StringRes val textRes: Int,
    val formatArgsText: String? = null,
) {

    /**
     * The filename of the Scan/s to be uploaded is blank
     */
    BlankFilename(R.string.scan_snackbar_incorrect_name),

    /**
     * The filename of the Scan/s to be uploaded contains invalid characters
     */
    FilenameWithInvalidCharacters(R.string.scan_snackbar_invalid_characters, "\" * / : < > ? |"),
}