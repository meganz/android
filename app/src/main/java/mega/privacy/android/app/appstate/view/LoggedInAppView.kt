package mega.privacy.android.app.appstate.view

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.appstate.AppStateViewModel
import mega.privacy.android.app.appstate.model.AppState

@Composable
internal fun LoggedInAppView(
    viewModel: AppStateViewModel
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    when (val appState = state.value) {
        is AppState.Data -> {
            MegaApp(
                navController = rememberNavController(),
                appState = appState,
                onInteraction = viewModel::signalPresence,
            )
        }

        AppState.Loading -> {}

        AppState.FetchingNodes -> {
            LoginInProgressView()
        }
    }
}