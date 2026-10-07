package mega.privacy.mobile.home.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Returns a callback that tells the user the action they tapped needs an internet connection.
 */
@Composable
internal fun rememberNoConnectionSnackbar(): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val snackBarEventQueue = rememberSnackBarQueue()
    return remember(coroutineScope, snackBarEventQueue) {
        {
            coroutineScope.launch {
                snackBarEventQueue.queueMessage(sharedR.string.check_internet_connection_error)
            }
        }
    }
}
