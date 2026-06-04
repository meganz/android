package mega.privacy.android.feature.videoeditor.presentation.screen

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.VideoEditorScreenNavKey

@OptIn(UnstableApi::class)
fun EntryProviderScope<NavKey>.videoEditorScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<VideoEditorScreenNavKey> { navKey ->
        VideoEditorRoute(nodeHandle = navKey.nodeHandle, onClose = navigationHandler::back)
    }
}
