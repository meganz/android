package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingImageToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingPDFToChatEvent

/**
 * A Composable holding all Save Scanned Documents screens using the Navigation Controller
 *
 * @param viewModel The ViewModel responsible for all business logic
 * @param onUploadScansStarted Lambda to indicate that the scanned document/s (through the provided
 * Uri) should begin uploading
 */
@Composable
internal fun SaveScannedDocumentsScreen(
    viewModel: SaveScannedDocumentsViewModel,
    onUploadToChat: (Uri, ScanFileType, comesFromChat: Boolean, canSelectScanFileType: Boolean) -> Unit,
    onUploadToCloudDrive: (Uri, ScanFileType, defaultNodeDestination: Long, canSelectScanFileType: Boolean) -> Unit,
) {
    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) { padding ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SaveScannedDocumentsView(
            uiState = uiState,
            onFilenameChanged = viewModel::onFilenameChanged,
            onFilenameConfirmed = viewModel::onFilenameConfirmed,
            onSaveButtonClicked = viewModel::onSaveButtonClicked,
            onScanDestinationSelected = viewModel::onScanDestinationSelected,
            onScanFileTypeSelected = viewModel::onScanFileTypeSelected,
            onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
            onUploadScansStarted = { uriToUpload ->
                val uiState = viewModel.uiState.value
                if (uiState.originatedFromChat) {
                    Analytics.tracker.trackEvent(
                        if (uiState.scanFileType == ScanFileType.Pdf) {
                            DocumentScannerUploadingPDFToChatEvent
                        } else {
                            DocumentScannerUploadingImageToChatEvent
                        }
                    )
                }
                if (uiState.scanDestination == ScanDestination.CloudDrive) {
                    onUploadToCloudDrive(
                        uriToUpload,
                        uiState.scanFileType,
                        uiState.cloudDriveParentHandle,
                        uiState.canSelectScanFileType
                    )
                } else {
                    onUploadToChat(
                        uriToUpload,
                        uiState.scanFileType,
                        uiState.originatedFromChat,
                        uiState.canSelectScanFileType
                    )
                }
            },
            onUploadScansEventConsumed = viewModel::onUploadScansEventConsumed,
        )
    }
}