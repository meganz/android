package mega.privacy.android.navigation.contract.queue.snackbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composable function to remember and access the [SnackbarEventQueue] in Compose code.
 *
 * This function provides access to the snackbar event queue, allowing you to queue snackbar messages
 * from within composable functions.
 *
 * @return The [SnackbarEventQueue] instance that can be used to queue snackbar messages.
 *
 * @example
 * ```
 * val snackbarQueue = rememberSnackBarQueue()
 * snackbarQueue.queueMessage("Hello, World!")
 * ```
 */
@Composable
fun rememberSnackBarQueue(): SnackbarEventQueue {
    val context = LocalContext.current
    return remember {
        context.snackbarEventQueue
    }
}

