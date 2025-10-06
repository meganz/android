package mega.privacy.android.app.presentation.logout

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class LogoutFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            logoutConfirmationDialogDestination(
                navigateBack = navigationHandler::back
            )
        }
}
