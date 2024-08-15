package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.documentscanner.navigation.routes.SaveScannedDocumentsRoute

/**
 * Route for [SaveScannedDocumentsRoute]
 */
internal const val SAVE_SCANNED_DOCUMENTS_ROUTE = "saveScannedDocuments/main"

/**
 * Builds the Navigation Graph of Save Scanned Documents
 */
internal fun NavGraphBuilder.saveScannedDocumentsScreen() {
    composable(SAVE_SCANNED_DOCUMENTS_ROUTE) {
        SaveScannedDocumentsRoute()
    }
}