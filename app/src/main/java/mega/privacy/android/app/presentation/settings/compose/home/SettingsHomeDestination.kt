package mega.privacy.android.app.presentation.settings.compose.home

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.home.view.SettingsHomeView

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
    navHostController: NavHostController,
) {
    composable<SettingsHome> { backStackEntry ->
        val args = backStackEntry.toRoute<SettingsHome>()
        val viewModel = hiltViewModel<SettingHomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsHomeView(
            state = state,
            initialKey = args.initialSetting,
            navHostController = navHostController,
        )
    }
}