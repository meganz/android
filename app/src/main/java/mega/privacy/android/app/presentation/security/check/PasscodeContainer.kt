package mega.privacy.android.app.presentation.security.check

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.passcode.view.PasscodeView
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import timber.log.Timber

@Composable
internal fun PasscodeContainer(
    passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    passcodeUI: @Composable () -> Unit = { PasscodeView(cryptObjectFactory = passcodeCryptObjectFactory) },
    viewModel: PasscodeCheckViewModel = viewModel(),
    canLock: () -> Boolean = { true },
    loading: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        Timber.d("PasscodeContainer: state changed: $state")
    }

    Box {
        if (loading != null && state is PasscodeCheckState.Loading) {
            loading()
        } else {
            content()
        }

        if (state is PasscodeCheckState.Locked && canLock()) {
            passcodeUI()
        }
    }
}
