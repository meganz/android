package mega.privacy.android.app.presentation.documentscanner.model

/**
 * The UI State for Save Scanned Documents
 *
 * @property filename The filename of the document containing all scans
 */
internal data class SaveScannedDocumentsUiState(
    val filename: String = "",
)
