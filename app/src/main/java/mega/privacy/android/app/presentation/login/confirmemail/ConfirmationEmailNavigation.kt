package mega.privacy.android.app.presentation.login.confirmemail

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationScreen
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationViewModel
import mega.privacy.android.app.presentation.login.confirmemail.view.NewConfirmEmailRoute
import mega.privacy.android.app.utils.Constants.HELP_CENTRE_HOME_URL
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression

@Serializable
data object ConfirmationEmailNavKey : NoSessionNavKey.Mandatory

internal fun EntryProviderScope<NavKey>.confirmationEmailScreen(
    navigationHandler: NavigationHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
) {
    entry<ConfirmationEmailNavKey>(
        metadata = buildMetadata { withOverlaySuppression() }
    ) { key ->
        val context = LocalContext.current
        val result by navigationHandler.monitorResult<String>(UpdateEmailForAccountCreationViewModel.EMAIL)
            .collectAsStateWithLifecycle("")
        NewConfirmEmailRoute(
            newEmail = result,
            onShowPendingFragment = sharedViewModel::setPendingFragmentToShow,
            onNavigateToChangeEmailAddress = { email, fullName ->
                navigationHandler.navigate(
                    UpdateEmailForAccountCreationScreen(
                        email = email,
                        fullName = fullName
                    )
                )
            },
            onNavigateToHelpCentre = {
                context.launchUrl(HELP_CENTRE_HOME_URL)
            },
            onBackPressed = onFinish,
            checkTemporalCredentials = sharedViewModel::checkTemporalCredentials,
            cancelCreateAccount = sharedViewModel::cancelCreateAccount,
            onSetTemporalEmail = sharedViewModel::setTemporalEmail
        )
    }
}
