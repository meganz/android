package mega.privacy.android.app.presentation.login.confirmemail

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.changeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.navigateToChangeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.app.presentation.login.confirmemail.view.confirmEmail
import mega.privacy.android.app.presentation.login.confirmemail.view.confirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Graph for Confirm Email screen.
 * It contains the Confirm Email screen and the Change Email Address screen.
 */
@Composable
fun NewConfirmEmailGraph(
    fullName: String,
    viewModel: ConfirmEmailViewModel,
    uiState: ConfirmEmailUiState,
    onShowPendingFragment: (fragmentType: LoginFragmentType) -> Unit,
    onSetTemporalEmail: (email: String) -> Unit,
    onNavigateToHelpCentre: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = confirmEmailRoute
    ) {
        confirmEmail(
            fullName = fullName,
            onShowPendingFragment = onShowPendingFragment,
            onSetTemporalEmail = onSetTemporalEmail,
            viewModel = viewModel,
            onNavigateToHelpCentre = onNavigateToHelpCentre,
            onNavigateToChangeEmailAddress = {
                navController.navigateToChangeEmailAddress(
                    email = uiState.registeredEmail.orEmpty(),
                    fullName = fullName
                )
            }
        )

        changeEmailAddress(
            onChangeEmailSuccess = {
                viewModel.updateRegisteredEmail(it)
                viewModel.showSnackBar(context.getString(sharedR.string.email_confirmation_email_address_update_message))
                navController.popBackStack()
            },
        )
    }
}