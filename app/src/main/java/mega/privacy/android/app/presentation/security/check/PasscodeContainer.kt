package mega.privacy.android.app.presentation.security.check

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.presentation.passcode.view.PasscodeDialog
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState

@Composable
internal fun PasscodeContainer(
    passcodeUI: @Composable () -> Unit = { PasscodeDialog() },
    viewModel: PasscodeCheckViewModel = viewModel(),
    content: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        PasscodeCheckState.Loading -> content()
        PasscodeCheckState.UnLocked -> content()
        PasscodeCheckState.Locked -> passcodeUI()
    }
}
