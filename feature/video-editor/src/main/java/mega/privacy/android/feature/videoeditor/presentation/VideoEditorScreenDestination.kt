package mega.privacy.android.feature.videoeditor.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.VideoEditorScreenNavKey

fun EntryProviderScope<NavKey>.videoEditorScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<VideoEditorScreenNavKey> {
        VideoEditorScreen()
    }
}
