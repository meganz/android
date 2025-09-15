package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.documentscanner.ValidateScanFilenameUseCase
import mega.privacy.android.domain.usecase.file.RenameFileAndDeleteOriginalUseCase
import mega.privacy.mobile.analytics.event.DocumentScannerSaveImageToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSaveImageToCloudDriveEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSavePDFToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSavePDFToCloudDriveEvent
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * The [ViewModel] for Save Scanned Documents
 *
 * @property validateScanFilenameUseCase Validates a given scan filename and returns the
 * corresponding validation status
 * @property renameFileAndDeleteOriginalUseCase Renames the original File, deletes it and returns
 * the renamed File
 * @property savedStateHandle The Saved State Handle
 */
@HiltViewModel
internal class SaveScannedDocumentsViewModel @Inject constructor(
    private val validateScanFilenameUseCase: ValidateScanFilenameUseCase,
    private val renameFileAndDeleteOriginalUseCase: RenameFileAndDeleteOriginalUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaveScannedDocumentsUiState())

    /**
     * The Save Scanned Documents UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow(
                    key = EXTRA_ORIGINATED_FROM_CHAT,
                    initialValue = false,
                ),
                savedStateHandle.getStateFlow(
                    key = EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                    initialValue = -1L,
                ),
                savedStateHandle.getStateFlow(
                    key = EXTRA_SCAN_PDF_URI,
                    initialValue = null
                ),
                savedStateHandle.getStateFlow(
                    key = EXTRA_SCAN_SOLO_IMAGE_URI,
                    initialValue = null,
                ),
                savedStateHandle.getStateFlow(
                    key = INITIAL_FILENAME_FORMAT,
                    initialValue = "",
                ),
            ) { originatedFromChat: Boolean, cloudDriveParentHandle: Long, pdfUri: Uri?, soloImageUri: Uri?, fileFormat ->
                { state: SaveScannedDocumentsUiState ->
                    val formattedDateTime = String.format(
                        Locale.getDefault(),
                        DATE_TIME_FORMAT,
                        Calendar.getInstance(),
                    )
                    val initialFilename = String.format(
                        Locale.getDefault(),
                        fileFormat,
                        formattedDateTime,
                    ) + _uiState.value.scanFileType.fileSuffix

                    state.copy(
                        cloudDriveParentHandle = cloudDriveParentHandle,
                        filename = initialFilename,
                        originatedFromChat = originatedFromChat,
                        pdfUri = pdfUri,
                        scanDestination = if (originatedFromChat) {
                            ScanDestination.Chat
                        } else {
                            ScanDestination.CloudDrive
                        },
                        soloImageUri = soloImageUri,
                    )
                }
            }.collect {
                _uiState.update(it)
            }
        }
    }

    /**
     * Updates the filename of the scanned Document/s to be uploaded and displays an Error Message
     * in the filename input if the new filename is invalid
     *
     * @param newFilename the new Filename
     */
    fun onFilenameChanged(newFilename: String) {
        val filenameValidationStatus = validateScanFilenameUseCase(
            filename = newFilename,
            fileExtension = _uiState.value.scanFileType.fileSuffix,
        )
        _uiState.update {
            it.copy(
                filename = newFilename,
                filenameValidationStatus = filenameValidationStatus,
            )
        }
    }

    /**
     * This function is called after deciding on a new filename and triggering an ImeAction.Done
     * Keyboard Event. If the new filename is invalid, a Snackbar is shown displaying an error
     * message
     *
     * @param newFilename the new Filename
     */
    fun onFilenameConfirmed(newFilename: String) = isConfirmedFilenameValid(newFilename)

    /**
     * Checks if the filename is valid before proceeding to save the Scan/s to the selected
     * destination
     */
    fun onSaveButtonClicked() {
        if (isConfirmedFilenameValid()) {
            val uiState = _uiState.value
            val uriToUpload = when (uiState.scanFileType) {
                ScanFileType.Pdf -> uiState.pdfUri
                ScanFileType.Jpg -> uiState.soloImageUri
            }
            uriToUpload?.let { nonNullUri ->
                val uriPath = nonNullUri.path
                uriPath?.let { nonNullUriPath ->
                    viewModelScope.launch {
                        runCatching {
                            renameFileAndDeleteOriginalUseCase(
                                originalUriPath = UriPath(nonNullUriPath),
                                newFilename = uiState.filename,
                            )
                        }.onSuccess { renamedFile ->
                            logDocumentScanEvent(uiState.scanFileType, uiState.scanDestination)
                            _uiState.update { it.copy(uploadScansEvent = triggered(renamedFile.toUri())) }
                        }.onFailure { exception ->
                            Timber.e("Unable to upload the scan/s due to a renaming issue:\n ${exception.printStackTrace()}")
                        }
                    }
                } ?: run {
                    Timber.e("Unable to upload the scan/s as the Uri path is missing")
                }
            } ?: run {
                Timber.e("Unable to upload the scan/s as the Uri is missing")
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun logDocumentScanEvent(
        scanFileType: ScanFileType,
        scanDestination: ScanDestination,
    ) {
        when {
            scanFileType == ScanFileType.Pdf && scanDestination == ScanDestination.CloudDrive ->
                Analytics.tracker.trackEvent(DocumentScannerSavePDFToCloudDriveEvent)

            scanFileType == ScanFileType.Pdf && scanDestination == ScanDestination.Chat ->
                Analytics.tracker.trackEvent(DocumentScannerSavePDFToChatEvent)

            scanFileType == ScanFileType.Jpg && scanDestination == ScanDestination.CloudDrive ->
                Analytics.tracker.trackEvent(DocumentScannerSaveImageToCloudDriveEvent)

            scanFileType == ScanFileType.Jpg && scanDestination == ScanDestination.Chat ->
                Analytics.tracker.trackEvent(DocumentScannerSaveImageToChatEvent)
        }
    }

    /**
     * When the new filename is decided, this checks if the filename is valid or not. If the filename
     * is invalid, the UI State is updated to show a Snackbar with an error message
     *
     * @param filename The filename to be checked. Defaults to the saved filename in the UI State if
     * it is not provided
     */
    private fun isConfirmedFilenameValid(filename: String = _uiState.value.filename): Boolean {
        return when (val filenameValidationStatus = validateScanFilenameUseCase(
            filename = filename,
            fileExtension = _uiState.value.scanFileType.fileSuffix,
        )) {
            ScanFilenameValidationStatus.ValidFilename -> true
            ScanFilenameValidationStatus.InvalidFilename -> false
            else -> {
                _uiState.update { it.copy(snackbarMessage = triggered(filenameValidationStatus)) }
                false
            }
        }
    }

    /**
     * Updates the Scan Destination of the scanned Document/s to be uploaded
     *
     * @param newScanDestination The new Scan Destination
     */
    fun onScanDestinationSelected(newScanDestination: ScanDestination) {
        _uiState.update { it.copy(scanDestination = newScanDestination) }
    }

    /**
     * When changing the [ScanFileType], update the Filename, Filename Validation Status and
     * Scan File Type to the UI State
     *
     * Filename Logic:
     *
     * * If the current Filename ends with the new [ScanFileType] file suffix, there are no changes
     *
     *     * Example: If the previous Filename is "Scanned.pdf" and PDF is the new [ScanFileType]
     *
     * * Else if the current Filename ends with the old [ScanFileType] file suffix, then replace it
     * with the new [ScanFileType] suffix
     *
     *     * Example: If the previous Filename is "Scanned.pdf" and JPG is the new [ScanFileType],
     *     then the new Filename is "Scanned.jpg".
     *
     *  * Else, simply append the new [ScanFileType] suffix to the current Filename
     *
     *     * Example: If the previous Filename is "Scanned" and PDF is the new [ScanFileType], then
     *     the new Filename is "Scanned.pdf"
     *
     * @param newScanFileType The new Scan File Type
     */
    fun onScanFileTypeSelected(newScanFileType: ScanFileType) {
        val previousSuffix = _uiState.value.scanFileType.fileSuffix
        val previousFilename = _uiState.value.filename
        val newSuffix = newScanFileType.fileSuffix

        val updatedFilename = when {
            previousFilename.endsWith(newSuffix) -> previousFilename
            previousFilename.endsWith(previousSuffix) -> {
                previousFilename.substringBeforeLast(
                    previousSuffix,
                    ""
                ) + newSuffix
            }

            else -> "$previousFilename$newSuffix"
        }
        val updatedFileValidationStatus = validateScanFilenameUseCase(
            filename = updatedFilename,
            fileExtension = newSuffix,
        )
        _uiState.update {
            it.copy(
                filename = updatedFilename,
                filenameValidationStatus = updatedFileValidationStatus,
                scanFileType = newScanFileType,
            )
        }
    }

    /**
     * Notifies the UI State that the Snackbar has been shown with the specific message
     */
    fun onSnackbarMessageConsumed() {
        _uiState.update { it.copy(snackbarMessage = consumed()) }
    }

    /**
     * Notifies the UI State that the upload scans event has been triggered
     */
    fun onUploadScansEventConsumed() {
        _uiState.update { it.copy(uploadScansEvent = consumed()) }
    }

    companion object {
        internal const val EXTRA_ORIGINATED_FROM_CHAT = "EXTRA_ORIGINATED_FROM_CHAT"
        internal const val EXTRA_CLOUD_DRIVE_PARENT_HANDLE = "EXTRA_CLOUD_DRIVE_PARENT_HANDLE"
        internal const val EXTRA_SCAN_PDF_URI = "EXTRA_SCAN_PDF_URI"
        internal const val EXTRA_SCAN_SOLO_IMAGE_URI = "EXTRA_SCAN_SOLO_IMAGE_URI"
        internal const val INITIAL_FILENAME_FORMAT = "INITIAL_FILENAME_FORMAT"

        internal const val DATE_TIME_FORMAT = "%1tY%<tm%<td%<tH%<tM%<tS"
    }
}