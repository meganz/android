package mega.privacy.android.app.presentation.settings.navigation.destination

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.home.SettingContainerViewModel
import mega.privacy.android.app.presentation.settings.home.view.SettingsHomeView

/**
 * Settings home
 *
 * @property initialSetting
 */
@Serializable
data class SettingsHome(val initialSetting: String? = null)

/**
 * Settings home
 *
 */
fun NavGraphBuilder.settingsHome(
    onBackPressed: () -> Unit,
) {
    composable<SettingsHome> { backStackEntry ->
        val viewModel = hiltViewModel<SettingContainerViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsHomeView(onBackPressed = onBackPressed)
    }
}