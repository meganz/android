package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.feature.clouddrive.presentation.cloudDriveScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler

class CloudDriveFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler) -> Unit =
        { navigationHandler ->
            cloudDriveScreen(navigationHandler::back)
        }
}