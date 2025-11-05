package mega.privacy.android.app.appstate.content.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.login.FetchNodesViewModel
import mega.privacy.android.app.presentation.login.view.FetchNodesContent

/**
 * Composable function to display the login in progress view.
 *
 * @param modifier The modifier to be applied to the view.
 * @param viewModel The [mega.privacy.android.app.presentation.login.FetchNodesViewModel] instance to manage the state of the view.
 */
@Composable
fun FetchingContentView(
    modifier: Modifier = Modifier,
    viewModel: FetchNodesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FetchNodesContent(
        modifier = modifier,
        isRequestStatusInProgress = state.isRequestStatusInProgress,
        currentProgress = state.currentProgress,
        currentStatusText = state.currentStatusText,
        startProgress = if (state.isFromLogin) 0.275f else 0f,
        requestStatusProgress = state.requestStatusProgress,
    )
}