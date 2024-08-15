package mega.privacy.android.app.presentation.documentscanner.navigation.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsView
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel

/**
 * A Route Composable to the main Save Scanned Documents screen
 *
 * @param viewModel The ViewModel responsible for all business logic
 */
@Composable
internal fun SaveScannedDocumentsRoute(
    viewModel: SaveScannedDocumentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SaveScannedDocumentsView(
        uiState = uiState,
    )
}