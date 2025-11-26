package mega.privacy.android.feature.chat.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class ChatFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            meetingHasEndedDialog(navigationHandler::back, navigationHandler::navigate)
        }
}