package mega.privacy.android.navigation.contract.queue.snackbar

import androidx.annotation.StringRes
import mega.android.core.ui.model.SnackbarAttributes

/**
 * Interface for queuing snackbar events in a single-activity architecture.
 *
 * This interface provides a mechanism to queue snackbar messages that can be consumed
 * by subscribers, allowing for decoupled communication between ViewModels and UI components.
 * The implementation uses a Channel-based approach for thread-safe event handling.
 */
interface SnackbarEventQueue {
    /**
     * Queues a simple snackbar message for consumption by subscribers.
     *
     * This method adds a string message to the internal queue. The message will be
     * available for consumption through the [SnackbarEventQueueReceiver.eventQueue] channel.
     *
     * @param message The message to be displayed in the snackbar
     */
    suspend fun queueMessage(message: String)

    /**
     * Queues a simple snackbar message for consumption by subscribers.
     *
     * This method adds a string message to the internal queue. The message will be
     * available for consumption through the [SnackbarEventQueueReceiver.eventQueue] channel.
     *
     * @param resId The res id of the message to be displayed in the snackbar
     * @param args The arguments to be used in the message
     */
    suspend fun queueMessage(@StringRes resId: Int, vararg args: Any)

    /**
     * Queues a snackbar message for consumption by subscribers.
     *
     * This method adds a snackbar event to the internal queue. The event will be
     * available for consumption through the [SnackbarEventQueueReceiver.eventQueue] channel.
     *
     * @param attributes The snackbar attributes containing the message and display options
     */
    suspend fun queueMessage(attributes: SnackbarAttributes)
}