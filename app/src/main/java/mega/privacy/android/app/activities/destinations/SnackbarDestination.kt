package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SnackbarNavKey

class SnackbarDestination(
    private val snackbarEventQueue: SnackbarEventQueue,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            entry<SnackbarNavKey>(
                metadata = transparentMetadata()
            ) { key ->
                val message = key.getMessage(LocalContext.current)
                LaunchedEffect(message) {
                    if (!message.isNullOrBlank()) {
                        snackbarEventQueue.queueMessage(message)
                    }
                }
                // Immediately pop this destination from the back stack to don't show the snackbar again
                navigationHandler.back()
            }
        }
}