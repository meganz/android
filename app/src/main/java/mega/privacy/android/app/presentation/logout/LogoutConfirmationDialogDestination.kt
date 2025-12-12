package mega.privacy.android.app.presentation.logout

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.transparent.transparentMetadata

@Serializable
data object LogoutConfirmationDialogM3NavKey : NavKey

fun EntryProviderScope<NavKey>.logoutConfirmationDialogDestination(
    navigateBack: () -> Unit,
) {
    entry<LogoutConfirmationDialogM3NavKey>(
        metadata = transparentMetadata()
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
