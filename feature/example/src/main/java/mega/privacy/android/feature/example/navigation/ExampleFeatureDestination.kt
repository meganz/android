package mega.privacy.android.feature.example.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.example.presentation.exampleLegacyResultScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class ExampleFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
//            exampleSecondaryScreen(navigationHandler::back)
//            exampleLegacyScreen(navigationHandler::back)
            exampleLegacyResultScreen(
                returnResult = navigationHandler::returnResult,
                onResultHandled = navigationHandler::clearResult
            )
        }

}