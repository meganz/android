package mega.privacy.android.app.appstate.view

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.login.LoginInProgressViewModel
import mega.privacy.android.app.presentation.login.model.LoginInProgressUiState
import mega.privacy.android.app.presentation.login.view.LoginInProgressContent

/**
 * Composable function to display the login in progress view.
 *
 * @param modifier The modifier to be applied to the view.
 * @param viewModel The [LoginInProgressViewModel] instance to manage the state of the view.
 */
@Composable
fun LoginInProgressView(
    modifier: Modifier = Modifier,
    viewModel: LoginInProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoginInProgressContent(
        modifier = modifier,
        isRequestStatusInProgress = state.isRequestStatusInProgress,
        currentProgress = state.currentProgress,
        currentStatusText = state.currentStatusText,
        requestStatusProgress = state.requestStatusProgress,
    )
}

/**
 * Text to show below progress bar
 */
@get:StringRes
val LoginInProgressUiState.currentStatusText: Int
    get() {
        val temporaryError = loginTemporaryError ?: fetchNodesUpdate?.temporaryError
        return when {
            temporaryError != null && !isRequestStatusInProgress -> temporaryError.messageId
            isFastLoginInProgress -> R.string.login_connecting_to_server
            (fetchNodesUpdate?.progress?.floatValue ?: 0f) > 0f -> R.string.login_preparing_filelist
            fetchNodesUpdate != null -> R.string.download_updating_filelist
            else -> R.string.login_connecting_to_server
        }
    }