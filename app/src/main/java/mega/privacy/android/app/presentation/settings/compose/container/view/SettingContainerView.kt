package mega.privacy.android.app.presentation.settings.compose.container.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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

    NavHost(
        navController = navHostController,
        startDestination = destination ?: EmptySettingsView,
        modifier = modifier
    ) {
        settingsGraph(
            navController = navHostController,
            featureSettings = state.nestedGraphs,
        )
    }
}
