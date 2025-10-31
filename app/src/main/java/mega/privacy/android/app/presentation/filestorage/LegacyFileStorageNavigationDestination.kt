package mega.privacy.android.app.presentation.filestorage

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyFileExplorerNavKey

fun EntryProviderScope<NavKey>.legacyFileStorageScreen(removeDestination: () -> Unit) {
    entry<LegacyFileExplorerNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = FileStorageActivity.getBrowseFilesIntent(
                context,
                key.uriPath,
                key.highlightedFiles
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

class FileStorageFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            legacyFileStorageScreen(navigationHandler::back)
        }
}

