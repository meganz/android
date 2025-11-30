package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey

fun EntryProviderScope<NavKey>.legacyMediaPlayerScreen(
    removeDestination: () -> Unit,
    mediaPlayerIntentMapper: MediaPlayerIntentMapper,
) {
    entry<LegacyMediaPlayerNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = mediaPlayerIntentMapper(
                context = context,
                contentUri = key.nodeContentUri,
                fileTypeInfo = key.fileTypeInfo,
                sortOrder = key.sortOrder,
                name = key.fileName,
                handle = key.fileHandle,
                parentHandle = key.parentHandle,
                isFolderLink = key.isFolderLink,
                viewType = key.nodeSourceType
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

