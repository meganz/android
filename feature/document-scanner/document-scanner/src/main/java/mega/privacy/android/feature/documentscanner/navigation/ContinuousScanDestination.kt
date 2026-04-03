package mega.privacy.android.feature.documentscanner.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.documentscanner.presentation.screen.ContinuousScanScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.ContinuousScanNavKey

/**
 * Feature destination for the continuous document scanner.
 */
class ContinuousScanDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(
        NavigationHandler,
        TransferHandler,
    ) -> Unit = { navigationHandler, _ ->
        continuousScanScreen(navigationHandler)
    }
}

private fun EntryProviderScope<NavKey>.continuousScanScreen(
    navigationHandler: NavigationHandler,
) {
    entry<ContinuousScanNavKey> {
        ContinuousScanScreen(
            onClose = { navigationHandler.back() },
        )
    }
}
