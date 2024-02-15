package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ForwardMessagesToChatsResult

/**
 * Converts the result of forwarding messages to a string.
 */
fun ForwardMessagesToChatsResult.toInfoText(context: Context) = when (this) {
    is ForwardMessagesToChatsResult.AllSucceeded -> context.resources.getQuantityString(
        R.plurals.messages_forwarded_success_plural,
        messagesCount,
    )

    is ForwardMessagesToChatsResult.AllNotAvailable -> context.resources.getQuantityString(
        R.plurals.messages_forwarded_error_not_available,
        messagesCount
    )

    is ForwardMessagesToChatsResult.SomeNotAvailable -> context.resources.getQuantityString(
        R.plurals.messages_forwarded_error_not_available,
        failuresCount,
    )

    is ForwardMessagesToChatsResult.AllFailed -> context.resources.getQuantityString(
        R.plurals.messages_forwarded_partial_error,
        messagesCount,
        messagesCount,
    )

    is ForwardMessagesToChatsResult.SomeFailed -> context.resources.getQuantityString(
        R.plurals.messages_forwarded_partial_error,
        failuresCount,
        failuresCount,
    )
}