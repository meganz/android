package mega.privacy.android.app.presentation.filelink

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey

fun EntryProviderScope<NavKey>.legacyFileLinkScreen(removeDestination: () -> Unit) {
    entry<LegacyFileLinkNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = FileLinkComposeActivity.getIntent(
                context,
                key.uriString?.toUri()
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
