package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerLaunchSourceHolder
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import javax.inject.Inject

/**
 * [FeatureDestination] that contributes the Compose video player route
 * ([ComposeVideoPlayerScreenNavKey]) to the single-activity navigation graph.
 */
class VideoPlayerFeatureDestination @Inject constructor(
    private val launchSourceHolder: VideoPlayerLaunchSourceHolder,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            composeVideoPlayerEntryProvider(
                navigationHandler = navigationHandler,
                launchSourceHolder = launchSourceHolder,
                onTransfer = transferHandler::setTransferEvent,
            )
        }
}
