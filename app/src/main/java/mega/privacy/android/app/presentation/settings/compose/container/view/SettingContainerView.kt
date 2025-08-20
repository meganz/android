package mega.privacy.android.app.presentation.settings.compose.container.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.presentation.settings.compose.container.SettingContainerViewModel
import mega.privacy.android.app.presentation.settings.compose.home.EmptySettingsView
import mega.privacy.android.app.presentation.settings.compose.navigation.settingsGraph

@Composable
internal fun SettingContainerView(
    navHostController: NavHostController,
    destination: Any?,
    modifier: Modifier = Modifier,
    viewModel: SettingContainerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigationHandler = remember(navHostController) { NavigationHandlerImpl(navHostController) }
    NavHost(
        navController = navHostController,
        startDestination = destination ?: EmptySettingsView,
        modifier = modifier
    ) {
        settingsGraph(
            navigationHandler = navigationHandler,
            featureSettings = state.nestedGraphs,
        )
    }
}
