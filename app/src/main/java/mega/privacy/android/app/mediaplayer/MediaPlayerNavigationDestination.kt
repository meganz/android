package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.app.presentation.videoplayer.Nav3VideoPlayerRouteLauncher
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.shared.resources.R
import timber.log.Timber

fun EntryProviderScope<NavKey>.legacyMediaPlayerScreen(
    removeDestination: () -> Unit,
    navigateToVideoRoute: (NavKey) -> Unit,
    nav3VideoPlayerRouteLauncher: Nav3VideoPlayerRouteLauncher,
    mediaPlayerIntentMapper: MediaPlayerIntentMapper,
    snackbarEventQueue: SnackbarEventQueue,
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
                viewType = key.nodeSourceType,
                searchedItems = key.searchedItems,
                nodeHandles = key.nodeHandles,
                mediaQueueTitle = key.mediaQueueTitle,
                collectionTitle = key.collectionTitle,
                collectionId = key.collectionId,
                publicLinkUrl = key.publicLinkUrl,
                localFilePath = key.localFilePath,
            )

            // Route to the Compose video player when the refactor flag is on; the transparent
            // launcher destination is replaced by the route (popUpTo inclusive) by the caller.
            val composeRouteKey = nav3VideoPlayerRouteLauncher.routeOrNull(intent)
            if (composeRouteKey != null) {
                navigateToVideoRoute(composeRouteKey)
                return@LaunchedEffect
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "Legacy media player: VIEW intent (data=${intent.data}, type=${intent.type})",
                    )
                    snackbarEventQueue.queueMessage(R.string.intent_not_available_file)
                }
            } else {
                snackbarEventQueue.queueMessage(R.string.intent_not_available_file)
            }

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

