package mega.privacy.android.app.appstate.view

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mega.privacy.android.app.appstate.AppStateViewModel
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.domain.entity.user.UserCredentials

@Composable
internal fun LoggedInAppView(navController: NavHostController, credentials: UserCredentials) {
    val viewModel = hiltViewModel<AppStateViewModel>()
    val state = viewModel.state.collectAsStateWithLifecycle()

    when (val appState = state.value) {
        is AppState.Data -> {
            MegaApp(
                navController = navController,
                appState = appState,
                onInteraction = viewModel::signalPresence,
            )
        }

        AppState.Loading -> {}
    }
}