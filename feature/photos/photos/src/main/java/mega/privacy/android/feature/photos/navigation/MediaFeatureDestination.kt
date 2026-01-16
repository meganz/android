package mega.privacy.android.feature.photos.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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

            videoPlaylistDetailScreen(navigationHandler)

            mediaSearchScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent
            )
            albumCoverSelectionScreen(navigationHandler = navigationHandler)
            albumPhotosSelectionScreen(navigationHandler = navigationHandler)
            albumDecryptionKey(navigationHandler = navigationHandler)
            cameraUploadsProgressRoute(
                modifier = Modifier.fillMaxSize(),
                onNavigateUp = navigationHandler::back
            )
        }
}
