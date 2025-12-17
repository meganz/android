package mega.privacy.android.app.getLink.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.HANDLE_LIST
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.GetLinkNavKey

/**
 * Navigation destination for GetLinkActivity that handles legacy navigation.
 *
 * Note: This navigates to the Activity only if no compose implementation exists.
 * If a compose implementation is available, it should be used instead.
 *
 * @param handles List of node handles to get their links
 */
fun EntryProviderScope<NavKey>.getLinkLegacyDestination(removeDestination: () -> Unit) {
    entry<GetLinkNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            // Validate that at least one handle is provided
            if (key.handles.isNotEmpty()) {
                val intent = Intent(context, GetLinkActivity::class.java).apply {
                    if (key.handles.size == 1) {
                        putExtra(HANDLE, key.handles[0])
                    } else {
                        putExtra(HANDLE_LIST, key.handles.toLongArray())
                    }
                }
                context.startActivity(intent)
            }
            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

