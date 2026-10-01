package mega.privacy.android.feature.mediaplayer.videoplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * [FeatureDestination] that contributes the revamped video player route
 * ([VideoPlayerScreenNavKey]) to the single-activity navigation graph.
 */
class VideoPlayerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ -> videoPlayerScreen(navigationHandler) }
}
