package mega.privacy.android.app.presentation.settings.passcode.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.serialization.Serializable
import mega.privacy.android.core.passcode.presentation.SetupPasscodeViewModel
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState
import mega.privacy.android.core.passcode.presentation.view.SetupPasscodeScreen

/**
 * Setup passcode destination
 *
 * @property isEdit true when changing an existing passcode, false when setting one for the first time
 */
@Serializable
internal data class SetupPasscodeDestination(val isEdit: Boolean)

internal fun NavGraphBuilder.setupPasscode(
    navController: NavHostController,
) {
    composable<SetupPasscodeDestination> { backStackEntry ->
        val viewModel = hiltViewModel<SetupPasscodeViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        EventEffect(
            event = (uiState as? SetupPasscodeUiState.Data)?.passcodeSavedEvent ?: consumed,
            onConsumed = viewModel::onPasscodeSavedEventConsumed,
        ) {
            navController.popBackStack()
        }

        SetupPasscodeScreen(
            uiState = uiState,
            isEdit = backStackEntry.toRoute<SetupPasscodeDestination>().isEdit,
            onPasscodeEntered = viewModel::onPasscodeEntered,
            onPasscodeTypeSelected = viewModel::onPasscodeTypeSelected,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
