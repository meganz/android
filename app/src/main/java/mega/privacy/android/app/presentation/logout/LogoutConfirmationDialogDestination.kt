package mega.privacy.android.app.presentation.logout

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable

@Serializable
data object LogoutConfirmationDialogM3NavKey : NavKey

fun EntryProviderBuilder<NavKey>.logoutConfirmationDialogDestination(
    navigateBack: () -> Unit,
) {
    entry<LogoutConfirmationDialogM3NavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Logout Confirmation Dialog"
            )
        )
    ) {
        val viewModel = hiltViewModel<LogoutViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        LogoutConfirmationDialogM3(
            logoutState = uiState,
            onLogout = {
                viewModel.logout()
            },
            onDismissed = {
                navigateBack()
            }
        )
    }
}
