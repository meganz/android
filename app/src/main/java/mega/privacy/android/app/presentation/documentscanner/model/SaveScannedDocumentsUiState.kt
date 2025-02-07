package mega.privacy.android.app.presentation.documentscanner.model

import android.net.Uri
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus

/**
 * The UI State for Save Scanned Documents
 *
 * @property cloudDriveParentHandle The Cloud Drive parent handle used to upload the scanned
 * document/s in Cloud Drive
 * @property filename The filename of the document containing all scans, which is changeable by the
 * User
 * @property filenameValidationStatus The filename validation status
 * @property originatedFromChat true if the Document Scanner was accessed from Chat
 * @property pdfUri The PDF Uri containing all scans
 * @property scanFileType The file type to upload the scanned document
 * @property scanDestination Specifies where to save the scans
 * @property snackbarMessage State Event that displays a Snackbar with a specific String when
 * triggered
 * @property soloImageUri The Image URI containing the scanned document
 * @property uploadScansEvent State Event signalling to upload the scanned document/s from the
 * provided Uri
 */
internal data class SaveScannedDocumentsUiState(
    val cloudDriveParentHandle: Long = -1L,
    val filename: String = "",
    val filenameValidationStatus: ScanFilenameValidationStatus? = null,
    val originatedFromChat: Boolean = false,
    val pdfUri: Uri? = null,
    val scanFileType: ScanFileType = ScanFileType.Pdf,
    val scanDestination: ScanDestination = ScanDestination.CloudDrive,
    val snackbarMessage: StateEventWithContent<ScanFilenameValidationStatus> = consumed(),
    val soloImageUri: Uri? = null,
    val uploadScansEvent: StateEventWithContent<Uri> = consumed(),
) {

    /**
     * True if the User can select the [scanFileType] to upload the scanned document/s
     */
    val canSelectScanFileType = soloImageUri != null
}
