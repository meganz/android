package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.FolderLinkNavKey

fun EntryProviderScope<NavKey>.folderLinkScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<FolderLinkNavKey> { args ->
        FolderLinkScreen(
            onBack = navigationHandler::back,
            onNavigate = navigationHandler::navigate
        )
    }
}
