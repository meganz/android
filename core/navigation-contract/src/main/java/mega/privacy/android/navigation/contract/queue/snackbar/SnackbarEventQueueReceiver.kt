package mega.privacy.android.navigation.contract.queue.snackbar

import kotlinx.coroutines.channels.ReceiveChannel
import mega.android.core.ui.model.SnackbarAttributes

/**
 * Interface for receiving snackbar events from the queue.
 *
 * This interface provides read-only access to the snackbar event queue,
 * allowing consumers to receive snackbar messages without the ability to emit new ones.
 *
 * @property eventQueue A [ReceiveChannel] that provides access to queued snackbar events.
 *                      Consumers can iterate over this channel to receive snackbar messages.
 */
interface SnackbarEventQueueReceiver {
    /**
     * A channel that provides access to queued snackbar events.
     */
    val eventQueue: ReceiveChannel<SnackbarAttributes>
}

