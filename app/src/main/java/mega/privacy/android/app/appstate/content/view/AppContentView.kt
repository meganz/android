package mega.privacy.android.app.appstate.content.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.model.AppContentState

@Composable
internal fun AppContentView(
    viewModel: AppContentStateViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val appState = state) {
        is AppContentState.Data -> {
            MegaApp(
                appContentState = appState,
                onInteraction = viewModel::signalPresence,
            )
        }

        AppContentState.Loading -> {}

        AppContentState.FetchingNodes -> {
            FetchingContentView()
        }
    }
}