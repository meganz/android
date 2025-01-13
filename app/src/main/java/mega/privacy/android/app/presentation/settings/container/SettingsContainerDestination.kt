package mega.privacy.android.app.presentation.settings.container

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.container.view.SettingContainerView

/**
 * Settings home
 *
 * @property initialSetting
 */
@Serializable
data class SettingsContainer(val initialSetting: String? = null)

/**
 * Settings home
 *
 */
fun NavGraphBuilder.settingsContainer(
    navHostController: NavHostController,
    onNavigateBack: () -> Unit,
) {
    composable<SettingsContainer> { backStackEntry ->
        val args = backStackEntry.toRoute<SettingsContainer>()
        val viewModel = hiltViewModel<SettingContainerViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingContainerView(
            state = state,
            initialKey = args.initialSetting,
            navHostController = navHostController,
            getTitle = viewModel::getScreenTitle,
            onNavigateBack = onNavigateBack,
        )
    }
}

