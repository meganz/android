package mega.privacy.android.app.consent

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler

data object ConsentDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, () -> Unit) -> Unit =
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