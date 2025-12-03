package mega.privacy.android.app.presentation.psa

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class PsaFeatureDestinations : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            standardPsaBottomSheetDestination(
                closePsaScreen = navigationHandler::remove,
                onNavigate = navigationHandler::navigate,
            )
            infoPsaBottomSheetDestination(
                closePsaScreen = navigationHandler::remove
            )
            webPsaDestination(
                closePsaScreen = navigationHandler::remove
            )
        }
}