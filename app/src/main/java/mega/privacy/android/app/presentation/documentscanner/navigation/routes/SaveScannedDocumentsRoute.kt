package mega.privacy.android.app.presentation.documentscanner.navigation.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsView
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel

/**
 * A Route Composable to the main Save Scanned Documents screen
 *
 * @param viewModel The ViewModel responsible for all business logic
 * @param onUploadScansStarted Lambda to indicate that the scanned document/s should begin uploading
 */
@Composable
internal fun SaveScannedDocumentsRoute(
    viewModel: SaveScannedDocumentsViewModel,
    onUploadScansStarted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SaveScannedDocumentsView(
        uiState = uiState,
        onFilenameChanged = viewModel::onFilenameChanged,
        onFilenameConfirmed = viewModel::onFilenameConfirmed,
        onSaveButtonClicked = viewModel::onSaveButtonClicked,
        onScanDestinationSelected = viewModel::onScanDestinationSelected,
        onScanFileTypeSelected = viewModel::onScanFileTypeSelected,
        onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
        onUploadScansStarted = onUploadScansStarted,
        onUploadScansEventConsumed = viewModel::onUploadScansEventConsumed,
    )
}