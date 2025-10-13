package mega.privacy.android.app.presentation.notification.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class NotificationsFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            notifications(navigationHandler)
        }
}