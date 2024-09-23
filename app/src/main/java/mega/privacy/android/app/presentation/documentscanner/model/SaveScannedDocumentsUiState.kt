package mega.privacy.android.app.presentation.documentscanner.model

import android.net.Uri
import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * The UI State for Save Scanned Documents
 *
 * @property cloudDriveParentHandle The Cloud Drive parent handle used to upload the scanned
 * document/s in Cloud Drive
 * @property filename The filename of the document containing all scans, which is changeable by the
 * User
 * @property filenameErrorMessage The error message shown in the filename input
 * @property pdfUri The PDF Uri containing all scans
 * @property scanFileType The file type to upload the scanned document
 * @property scanDestination Specifies where to save the scans
 * @property snackbarMessage State Event that displays a Snackbar with a specific String when
 * triggered
 * @property soloImageUri The Image URI containing the scanned document
 * @property uploadScansEvent State Event signalling to upload the scanned document/s
 */
internal data class SaveScannedDocumentsUiState(
    val cloudDriveParentHandle: Long = -1L,
    val filename: String = "",
    @StringRes val filenameErrorMessage: Int? = null,
    val pdfUri: Uri? = null,
    val scanFileType: ScanFileType = ScanFileType.Pdf,
    val scanDestination: ScanDestination = ScanDestination.CloudDrive,
    val snackbarMessage: StateEventWithContent<SaveScannedDocumentsSnackbarMessageUiItem> = consumed(),
    val soloImageUri: Uri? = null,
    val uploadScansEvent: StateEvent = consumed,
) {

    /**
     * True if the User can select the [scanFileType] to upload the scanned document/s
     */
    val canSelectScanFileType = soloImageUri != null

    /**
     * The actual filename that will be used when uploading the scanned document/s
     */
    val actualFilename = "$filename${scanFileType.fileSuffix}"
}
