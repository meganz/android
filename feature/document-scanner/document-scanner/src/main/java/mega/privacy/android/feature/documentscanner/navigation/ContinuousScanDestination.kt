package mega.privacy.android.feature.documentscanner.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.documentscanner.presentation.screen.ScannerRouterScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.ContinuousScanNavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata

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
    // Transparent so the confirmation dialog dims the screen the user came from
    // instead of a solid black backdrop. The loading and camera states draw their
    // own full-screen background over it.
    entry<ContinuousScanNavKey>(
        metadata = transparentMetadata(),
    ) {
        ScannerRouterScreen(navigationHandler = navigationHandler)
    }
}
