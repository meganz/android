package mega.privacy.android.feature.documentscanner.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.documentscanner.presentation.screen.ContinuousScanScreen
import mega.privacy.android.feature.documentscanner.presentation.screen.ScanReviewScreen
import mega.privacy.android.feature.documentscanner.presentation.screen.ScannerRouterScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.ContinuousScanNavKey
import mega.privacy.android.navigation.contract.navkey.RetakeScanNavKey
import mega.privacy.android.navigation.contract.navkey.ScanReviewNavKey
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
        scanReviewScreen(navigationHandler)
        retakeScanScreen(navigationHandler)
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

private fun EntryProviderScope<NavKey>.scanReviewScreen(
    navigationHandler: NavigationHandler,
) {
    entry<ScanReviewNavKey> {
        ScanReviewScreen(
            onBack = { navigationHandler.back() },
            onRetakePage = { pageId -> navigationHandler.navigate(RetakeScanNavKey(pageId)) },
        )
    }
}

private fun EntryProviderScope<NavKey>.retakeScanScreen(
    navigationHandler: NavigationHandler,
) {
    // The model is already downloaded by the time a page exists to retake, so this
    // shows the camera directly (no launch-mode routing) in retake mode. The first
    // capture replaces the page and pops back to the review screen.
    entry<RetakeScanNavKey> { key ->
        ContinuousScanScreen(
            onClose = { navigationHandler.back() },
            onSwitchToLegacy = {},
            onReviewPages = {},
            retakePageId = key.pageId,
            onRetakeDone = { navigationHandler.back() },
        )
    }
}
