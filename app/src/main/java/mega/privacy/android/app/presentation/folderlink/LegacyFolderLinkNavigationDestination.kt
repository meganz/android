package mega.privacy.android.app.presentation.folderlink

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey

fun EntryProviderScope<NavKey>.legacyFolderLinkScreen(removeDestination: () -> Unit) {
    entry<LegacyFolderLinkNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = FolderLinkComposeActivity.getIntent(
                context,
                key.uriString?.toUri()
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
