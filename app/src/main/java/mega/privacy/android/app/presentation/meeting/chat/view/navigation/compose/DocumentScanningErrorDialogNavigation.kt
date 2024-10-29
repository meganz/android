package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.documentscanner.dialogs.DocumentScanningErrorDialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel

internal fun NavGraphBuilder.documentScanningErrorDialog(
    navController: NavHostController,
) {
    dialog(route = "documentScanningError") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        DocumentScanningErrorDialog(
            documentScanningError = uiState.documentScanningError,
            onErrorAcknowledged = {
                viewModel.onDocumentScanningErrorConsumed()
                navController.popBackStack()
            },
            onErrorDismissed = {
                viewModel.onDocumentScanningErrorConsumed()
                navController.popBackStack()
            },
        )
    }
}

internal fun NavHostController.navigateToDocumentScanningErrorDialog(navOptions: NavOptions? = null) {
    navigate("documentScanningError", navOptions)
}