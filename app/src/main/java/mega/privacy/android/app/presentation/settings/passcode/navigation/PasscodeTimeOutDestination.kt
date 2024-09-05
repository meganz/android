package mega.privacy.android.app.presentation.settings.passcode.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.settings.passcode.PasscodeTimeoutViewModel
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.app.presentation.settings.passcode.view.PasscodeTimeoutDialog

/**
 * Passcode time out destination
 */
internal const val PasscodeTimeOutDestination = "PasscodeTimeOutDestination"

internal fun NavGraphBuilder.passCodeTimeOut(
    navController: NavHostController,
) {
    dialog(PasscodeTimeOutDestination) {
        val viewModel = hiltViewModel<PasscodeTimeoutViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        PasscodeTimeoutDialog(
            state = uiState,
            onTimeoutSelected = { option: TimeoutOption ->
                viewModel.onTimeoutSelected(option)
                navController.popBackStack()
            },
            onDismiss = navController::popBackStack
        )
    }
}
