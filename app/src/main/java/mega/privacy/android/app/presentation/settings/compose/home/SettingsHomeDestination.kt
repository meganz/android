package mega.privacy.android.app.presentation.settings.compose.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.home.view.SettingsHomeView

/**
 * Settings home
 *
 * @property initialSetting
 */
@Serializable
data object SettingsHome

/**
 * Settings home
 *
 */
fun NavGraphBuilder.settingsHome(
    onBackPressed: () -> Unit,
) {
    composable<SettingsHome> { backStackEntry ->
        val viewModel = hiltViewModel<SettingHomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsHomeView(
            state = state,
            onBackPressed = onBackPressed,
        )
    }
}

/**
 * This is used while we do not have a global graph to which we can add the settings home destination above
 */
@Composable
fun SettingsHomeDestinationWrapper(onBackPressed: () -> Unit) {
    val viewModel = hiltViewModel<SettingHomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsHomeView(
        state = state,
        onBackPressed = onBackPressed,
    )
}