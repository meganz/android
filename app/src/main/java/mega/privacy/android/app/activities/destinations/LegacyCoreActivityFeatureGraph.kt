package mega.privacy.android.app.activities.destinations

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.chat.navigation.chatLegacyDestination
import mega.privacy.android.app.presentation.contact.navigation.contactsLegacyDestination
import mega.privacy.android.app.presentation.search.navigation.searchLegacyDestination
import mega.privacy.android.app.presentation.settings.cookieSettingsNavigationDestination
import mega.privacy.android.app.presentation.testpassword.navigation.testPasswordLegacyDestination
import mega.privacy.android.feature.payment.upgradeAccount
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LegacyCoreActivityFeatureGraph : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            overDiskQuotaPaywallWarning(navigationHandler::back)
            upgradeAccount(navigationHandler::back)
            myAccount(navigationHandler::back)
            achievement(navigationHandler::back)
            webDestinations(navigationHandler::back)
            cookieSettingsNavigationDestination(navigationHandler::back)
            searchLegacyDestination(navigationHandler::back)
            contactsLegacyDestination(navigationHandler::back)
            chatLegacyDestination(navigationHandler::back)
            testPasswordLegacyDestination(navigationHandler::back)
        }
}