package mega.privacy.android.app.presentation.filecontact.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.contact.navigation.addContactLegacyDestination
import mega.privacy.android.app.presentation.contactinfo.navigation.contactInfoLegacyDestination
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class FileContactFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
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