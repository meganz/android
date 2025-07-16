package mega.privacy.android.feature.example.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.feature.example.presentation.exampleLegacyResultScreen
import mega.privacy.android.feature.example.presentation.exampleLegacyScreen
import mega.privacy.android.feature.example.presentation.exampleSecondaryScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler

class ExampleFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler) -> Unit =
        { navigationHandler ->
            exampleSecondaryScreen(navigationHandler::back)
            exampleLegacyScreen(navigationHandler::back)
            exampleLegacyResultScreen(navigationHandler::returnResult)
        }

}