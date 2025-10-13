package mega.privacy.android.app.presentation.settings.compose.container.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.settings.compose.container.SettingContainerViewModel

@Composable
internal fun SettingContainerView(
    navHostController: NavHostController,
    destination: Any?,
    modifier: Modifier = Modifier,
    viewModel: SettingContainerViewModel = hiltViewModel(),
) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//    val navigationHandler = remember(navHostController) { NavigationHandlerImpl(navHostController) }
//    NavHost(
//        navController = navHostController,
//        startDestination = destination ?: EmptySettingsView,
//        modifier = modifier
//    ) {
//        settingsGraph(
//            navigationHandler = navigationHandler,
//            featureSettings = state.nestedGraphs,
//        )
//    }
}
