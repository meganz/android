package mega.privacy.android.feature.photos.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class MediaFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            albumContentScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
                resultFlow = navigationHandler::monitorResult
            )
            mediaSearchScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent
            )
        }
}