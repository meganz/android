package mega.privacy.android.feature.chat.settings.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.chat.settings.calls.CallSettingsViewModel
import mega.privacy.android.feature.chat.settings.calls.view.CallSettingsScreen

@Serializable
internal object CallSettingsHome

internal fun NavGraphBuilder.callSettingsHome(onBackPressed: () -> Unit) {
    composable<CallSettingsHome> {
        val viewModel: CallSettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CallSettingsScreen(
            uiState = uiState,
            onSoundNavigationChanged = viewModel::setSoundNotification,
            onBackPressed = onBackPressed,
        )
    }
}