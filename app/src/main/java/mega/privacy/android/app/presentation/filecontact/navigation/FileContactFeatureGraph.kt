package mega.privacy.android.app.presentation.filecontact.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.navigation.addContactLegacyDestination
import mega.privacy.android.app.presentation.contactinfo.navigation.contactInfoLegacyDestination
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class FileContactFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            fileContacts(
                onNavigateBack = navigationHandler::back,
                onNavigate = navigationHandler::navigate,
                resultFlow = navigationHandler::monitorResult
            )
            addContactLegacyDestination(
                returnResult = navigationHandler::returnResult,
            )
            contactInfoLegacyDestination(
                removeDestination = navigationHandler::back
            )
        }

}