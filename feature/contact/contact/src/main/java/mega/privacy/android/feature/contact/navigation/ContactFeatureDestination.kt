package mega.privacy.android.feature.contact.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Contact feature destination
 */
class ContactFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit =
        { _, _ -> }
}

/**
 * Contact feature dialog destinations
 */
class ContactFeatureDialogDestinations() : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit =
        {navigationHandler, onHandled ->
            cannotVerifyContactDialogM3(navigationHandler::remove, onHandled)
        }
}
