package mega.privacy.android.app.presentation.login.confirmemail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.changeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.navigateToChangeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.view.confirmEmail
import mega.privacy.android.app.presentation.login.confirmemail.view.confirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants.HELP_CENTRE_HOME_URL
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Graph for Confirm Email screen.
 * It contains the Confirm Email screen and the Change Email Address screen.
 */
@Composable
fun NewConfirmEmailGraph(
    activityViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
    viewModel: ConfirmEmailViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBackPressed)

    LaunchedEffect(uiState.isCreatingAccountCancelled) {
        if (uiState.isCreatingAccountCancelled) {
            activityViewModel.cancelCreateAccount()
            viewModel.onHandleCancelCreateAccount()
        }
    }

    LaunchedEffect(uiState.isAccountConfirmed) {
        if (uiState.isAccountConfirmed) {
            activityViewModel.setPendingFragmentToShow(LoginFragmentType.Login)
            activityViewModel.checkTemporalCredentials()
        }
    }

    NavHost(
        navController = navController,
        startDestination = confirmEmailRoute
    ) {
        confirmEmail(
            fullName = uiState.firstName.orEmpty(),
            onShowPendingFragment = activityViewModel::setPendingFragmentToShow,
            onSetTemporalEmail = activityViewModel::setTemporalEmail,
            viewModel = viewModel,
            onNavigateToHelpCentre = { context.launchUrl(HELP_CENTRE_HOME_URL) },
            onNavigateToChangeEmailAddress = {
                navController.navigateToChangeEmailAddress(
                    email = uiState.registeredEmail.orEmpty(),
                    fullName = uiState.firstName.orEmpty(),
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