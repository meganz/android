package mega.privacy.android.feature.signin.external.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import mega.privacy.android.domain.exception.login.GoogleSignInException

/**
 * Stable handle that triggers Google Sign-In when invoked.
 *
 * Obtain one via [rememberGoogleSignInLauncher].
 */
@Immutable
class GoogleSignInLauncher internal constructor(
    private val launch: () -> Unit,
) {
    /**
     * Starts the Google Sign-In flow; results are reported through the callbacks
     * supplied to [rememberGoogleSignInLauncher].
     */
    operator fun invoke() = launch()
}

/**
 * Returns a [GoogleSignInLauncher] that launches Google Sign-In via Credential
 * Manager and reports the resulting ID token (or the failure cause) via the
 * supplied callbacks.
 *
 * The launcher is a no-op when there is no current [LocalActivity], and
 * silently swallows [GoogleSignInException.Cancelled].
 *
 * @param onIdToken Invoked on success with the raw Google ID token JWT.
 * @param onError Invoked with the cause when sign-in fails for any
 *   non-cancellation reason.
 */
@Composable
fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onError: (Throwable) -> Unit,
): GoogleSignInLauncher {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentOnIdToken by rememberUpdatedState(onIdToken)
    val currentOnError by rememberUpdatedState(onError)
    return remember(activity) {
        GoogleSignInLauncher {
            activity ?: return@GoogleSignInLauncher
            scope.launch {
                runCatching { activity.getGoogleIdToken() }
                    .onSuccess { token -> currentOnIdToken(token) }
                    .onFailure { error ->
                        if (error !is GoogleSignInException.Cancelled) {
                            currentOnError(error)
                        }
                    }
            }
        }
    }
}
