package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.app.presentation.meeting.chat.model.ForwardMessagesToChatsResult

/**
 * Get the chat id to open after forwarding messages to chats.
 */
fun ForwardMessagesToChatsResult.getOpenChatId(currentChatId: Long): Long? = with(this) {
    when {
        this is ForwardMessagesToChatsResult.AllSucceeded && chatId != null -> chatId
        this is ForwardMessagesToChatsResult.SomeFailed && chatId != null -> chatId
        this is ForwardMessagesToChatsResult.SomeNotAvailable && chatId != null -> chatId
        else -> {
            timber.log.Timber.e("No need to open a new chat fragment for this result.")
            null
        }
    }?.let { openChatId ->
        if (openChatId != currentChatId) {
            openChatId
        } else {
            null
        }
    }
}