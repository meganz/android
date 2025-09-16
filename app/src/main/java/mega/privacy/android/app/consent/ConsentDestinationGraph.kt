package mega.privacy.android.app.consent

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler

data object ConsentDialogDestinations : AppDialogDestinations {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            cookieDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled,
            )
            adConsentDialogDestination(
                navigateBack = navigationHandler::back,
                onDialogHandled = onHandled,
            )
        }
}