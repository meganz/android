package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * A Composable that builds the [NavHostController] for Save Scanned Documents
 *
 * @param modifier The [Modifier]
 * @param navHostController The [NavHostController] of the feature
 */
@Composable
internal fun SaveScannedDocumentsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = SAVE_SCANNED_DOCUMENTS_ROUTE,
    ) {
        saveScannedDocumentsScreen()
    }
}