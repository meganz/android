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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsSnackbarMessageUiItem
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.documentscanner.IsScanFilenameValidUseCase
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
 * @property isScanFilenameValidUseCase Checks whether the filename of the scanned Document/s is valid
 * @property renameFileAndDeleteOriginalUseCase Renames the original File, deletes it and returns
 * the renamed File
 * @property savedStateHandle The Saved State Handle
 */
@HiltViewModel
internal class SaveScannedDocumentsViewModel @Inject constructor(
    private val isScanFilenameValidUseCase: IsScanFilenameValidUseCase,
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
                    key = SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT,
                    initialValue = false,
                ),
                savedStateHandle.getStateFlow(
                    key = SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                    initialValue = -1L,
                ),
                savedStateHandle.getStateFlow(
                    key = SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI,
                    initialValue = null
                ),
                savedStateHandle.getStateFlow(
                    key = SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI,
                    initialValue = null,
                ),
            ) { originatedFromChat: Boolean, cloudDriveParentHandle: Long, pdfUri: Uri?, soloImageUri: Uri? ->
                { state: SaveScannedDocumentsUiState ->
                    state.copy(
                        originatedFromChat = originatedFromChat,
                        cloudDriveParentHandle = cloudDriveParentHandle,
                        filename = String.format(
                            Locale.getDefault(),
                            INITIAL_FILENAME_FORMAT,
                            Calendar.getInstance(),
                        ),
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
     * @param filename the new Filename
     */
    fun onFilenameChanged(filename: String) {
        _uiState.update {
            it.copy(
                filename = filename,
                filenameErrorMessage = getFilenameErrorMessage(filename),
            )
        }
    }

    /**
     * Retrieves the Error Message associated with the invalid Filename
     *
     * @param filename The filename to be checked.
     * @return A String resource specifying the type of invalid Filename, or null if the Filename is
     * valid
     */
    private fun getFilenameErrorMessage(filename: String = _uiState.value.filename) = when {
        isScanFilenameValidUseCase(filename) -> null
        filename.isBlank() -> R.string.scan_incorrect_name
        else -> R.string.scan_invalid_characters
    }

    /**
     * This function is called after deciding on a new filename and triggering an ImeAction.Done
     * Keyboard Event. If the new filename is invalid, a Snackbar is shown displaying an error
     * message
     *
     * @param filename the new Filename
     */
    fun onFilenameConfirmed(filename: String) = isConfirmedFilenameValid(filename)

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
                                newFilename = uiState.actualFilename,
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
    private fun isConfirmedFilenameValid(filename: String = _uiState.value.filename) =
        if (isScanFilenameValidUseCase(filename)) {
            true
        } else {
            _uiState.update {
                it.copy(
                    snackbarMessage = triggered(
                        if (filename.isBlank()) {
                            SaveScannedDocumentsSnackbarMessageUiItem.BlankFilename
                        } else {
                            SaveScannedDocumentsSnackbarMessageUiItem.FilenameWithInvalidCharacters
                        }
                    )
                )
            }
            false
        }

    /**
     * Updates the Scan Destination of the scanned Document/s to be uploaded
     *
     * @param scanDestination The new Scan Destination
     */
    fun onScanDestinationSelected(scanDestination: ScanDestination) {
        _uiState.update { it.copy(scanDestination = scanDestination) }
    }

    /**
     * Updates the File Type of the scanned Document/s to be uploaded
     *
     * @param scanFileType The new Scan File Type
     */
    fun onScanFileTypeSelected(scanFileType: ScanFileType) {
        _uiState.update { it.copy(scanFileType = scanFileType) }
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
        private const val INITIAL_FILENAME_FORMAT = "Scanned_%1tY%<tm%<td%<tH%<tM%<tS"
    }
}