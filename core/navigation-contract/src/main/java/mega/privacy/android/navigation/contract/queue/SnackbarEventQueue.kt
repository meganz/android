package mega.privacy.android.navigation.contract.queue

import kotlinx.coroutines.channels.ReceiveChannel
import mega.android.core.ui.model.SnackbarAttributes

/**
 * Interface for queuing snackbar events in a single-activity architecture.
 *
 * This interface provides a mechanism to queue snackbar messages that can be consumed
 * by subscribers, allowing for decoupled communication between ViewModels and UI components.
 * The implementation uses a Channel-based approach for thread-safe event handling.
 *
 * @property eventQueue A [ReceiveChannel] that provides access to queued snackbar events.
 *                      Consumers can iterate over this channel to receive snackbar messages.
 */
interface SnackbarEventQueue {
    /**
     * A channel that provides access to queued snackbar events.
     */
    val eventQueue: ReceiveChannel<SnackbarAttributes>

    /**
     * Queues a simple snackbar message for consumption by subscribers.
     *
     * This method adds a string message to the internal queue. The message will be
     * available for consumption through the [eventQueue] channel.
     *
     * @param message The message to be displayed in the snackbar
     */
    suspend fun queueMessage(message: String)

    /**
     * Queues a snackbar message for consumption by subscribers.
     *
     * This method adds a snackbar event to the internal queue. The event will be
     * available for consumption through the [eventQueue] channel.
     *
     * @param attributes The snackbar attributes containing the message and display options
     */
    suspend fun queueMessage(attributes: SnackbarAttributes)
}