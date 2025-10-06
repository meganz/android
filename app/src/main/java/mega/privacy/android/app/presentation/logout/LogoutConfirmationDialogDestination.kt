package mega.privacy.android.app.presentation.logout

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LogoutConfirmationDialogM3NavKey : NavKey

fun NavGraphBuilder.logoutConfirmationDialogDestination(
    navigateBack: () -> Unit,
) {
    dialog<LogoutConfirmationDialogM3NavKey> {
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
