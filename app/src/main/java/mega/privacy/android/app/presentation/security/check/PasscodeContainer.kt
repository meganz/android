package mega.privacy.android.app.presentation.security.check

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.presentation.passcode.view.PasscodeView
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState

@Composable
internal fun PasscodeContainer(
    passcodeUI: @Composable () -> Unit = { PasscodeView() },
    viewModel: PasscodeCheckViewModel = viewModel(),
    content: @Composable () -> Unit = {},
    loading: @Composable (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        PasscodeCheckState.Loading -> (loading ?: content).invoke()
        PasscodeCheckState.UnLocked -> content()
        PasscodeCheckState.Locked -> passcodeUI()
    }
}
