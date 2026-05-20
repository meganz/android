package mega.privacy.android.app.presentation.security.check

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.core.passcode.check.PasscodeCheckViewModel
import mega.privacy.android.core.passcode.check.model.PasscodeCheckState
import mega.privacy.android.core.passcode.presentation.view.PasscodeLoadingView
import mega.privacy.android.core.passcode.presentation.view.PasscodeView
import timber.log.Timber

/**
 * Passcode container
 *
 * @param passcodeUI
 * @param viewModel
 * @param canLock
 * @param loading
 * @param content
 */
@Composable
internal fun PasscodeContainer(
    passcodeUI: @Composable () -> Unit = {
        PasscodeView(
            logoutConfirmationDialog = { onDismissed ->
                LogoutConfirmationDialog(onDismissed = onDismissed)
            },
        )
    },
    viewModel: PasscodeCheckViewModel = hiltViewModel(),
    canLock: () -> Boolean = { true },
    loading: @Composable (() -> Unit) = { PasscodeLoadingView() },
    content: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        Timber.d("PasscodeContainer: state changed: $state")
    }

    Box {
        when (state) {
            is PasscodeCheckState.Locked -> {
                if (canLock()) {
                    Timber.d("PasscodeContainer: canLock() == true, showing lock screen.")
                    passcodeUI()
                } else {
                    Timber.d("PasscodeContainer: canLock() == false, showing content")
                    content()
                }
            }

            is PasscodeCheckState.Loading -> {
                Timber.d("PasscodeContainer: unlocked")
                loading()
            }

            PasscodeCheckState.UnLocked -> {
                content()
            }
        }
    }
}
