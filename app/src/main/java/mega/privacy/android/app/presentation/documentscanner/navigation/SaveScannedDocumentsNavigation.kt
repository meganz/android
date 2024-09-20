package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel
import mega.privacy.android.app.presentation.documentscanner.navigation.routes.SaveScannedDocumentsRoute

/**
 * Route for [SaveScannedDocumentsRoute]
 */
internal const val SAVE_SCANNED_DOCUMENTS_ROUTE = "saveScannedDocuments/main"

/**
 * Builds the Navigation Graph of Save Scanned Documents
 *
 * @param viewModel The ViewModel responsible for all business logic
 * @param onUploadScansStarted Lambda to indicate that the scanned document/s should begin uploading
 */
internal fun NavGraphBuilder.saveScannedDocumentsScreen(
    viewModel: SaveScannedDocumentsViewModel,
    onUploadScansStarted: () -> Unit,
) {
    composable(SAVE_SCANNED_DOCUMENTS_ROUTE) {
        SaveScannedDocumentsRoute(
            viewModel = viewModel,
            onUploadScansStarted = onUploadScansStarted,
        )
    }
}