package mega.privacy.android.app.consent

import androidx.navigation3.runtime.EntryProviderScope
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

data object ConsentDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            cookieDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled,
            )
            adConsentDialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled,
            )
        }
}