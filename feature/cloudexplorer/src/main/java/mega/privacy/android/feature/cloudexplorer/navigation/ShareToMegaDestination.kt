package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ShareToMegaNavKey

class ShareToMegaDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareToMegaDestination(navigationHandler::remove)
        }

    fun EntryProviderScope<NavKey>.shareToMegaDestination(
        removeDestination: (NavKey) -> Unit,
    ) {
        entry<ShareToMegaNavKey>(
            metadata = transparentMetadata()
        ) { key ->

        }
    }
}