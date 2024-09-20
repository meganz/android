package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel

/**
 * A Composable that builds the [NavHostController] for Save Scanned Documents
 *
 * @param modifier The [Modifier]
 * @param navHostController The [NavHostController] of the feature
 * @param viewModel The ViewModel responsible for all business logic
 * @param onUploadScansStarted Lambda to indicate that the scanned document/s should begin uploading
 */
@Composable
internal fun SaveScannedDocumentsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
    viewModel: SaveScannedDocumentsViewModel = hiltViewModel(),
    onUploadScansStarted: () -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = SAVE_SCANNED_DOCUMENTS_ROUTE,
    ) {
        saveScannedDocumentsScreen(
            viewModel = viewModel,
            onUploadScansStarted = onUploadScansStarted,
        )
    }
}