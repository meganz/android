package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerLaunchSourceHolder
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Registers the Compose video player route ([ComposeVideoPlayerScreenNavKey]). The play queue and
 * subtitle selection are rendered as in-place overlays inside that single entry, so there are no
 * additional destinations to register here.
 */
internal fun EntryProviderScope<NavKey>.composeVideoPlayerEntryProvider(
    navigationHandler: NavigationHandler,
    launchSourceHolder: VideoPlayerLaunchSourceHolder,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    composeVideoPlayerScreen(
        navigationHandler = navigationHandler,
        launchSourceHolder = launchSourceHolder,
        onTransfer = onTransfer,
    )
}
