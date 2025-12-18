package mega.privacy.android.app.presentation.photos.mediadiscovery.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.MediaDiscoveryNavKey

/**
 * Navigation destination for MediaDiscoveryActivity that handles legacy navigation.
 *
 * Note: This navigates to the Activity only if no compose implementation exists.
 * If a compose implementation is available, it should be used instead.
 */
fun EntryProviderScope<NavKey>.mediaDiscoveryLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<MediaDiscoveryNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            MediaDiscoveryActivity.startMDActivity(
                context = context,
                mediaHandle = key.nodeHandle,
                folderName = key.nodeName.orEmpty(),
                isFromFolderLink = key.isFromFolderLink,
            )

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

