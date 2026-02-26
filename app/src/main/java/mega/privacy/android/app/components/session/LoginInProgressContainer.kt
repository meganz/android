package mega.privacy.android.app.components.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Login in progress container that ensures content is only shown when login is not in progress.
 * When login mutex is locked, [loadingView] is shown instead of [content].
 *
 * @param viewModel
 * @param loadingView Composable shown while login is in progress. Use { } for no loading indicator.
 * @param content Content to show when login is not in progress
 */
@Composable
fun LoginInProgressContainer(
    viewModel: LoginInProgressViewModel = hiltViewModel(),
    loadingView: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoginInProgress) {
        loadingView()
    } else {
        content()
    }
}
