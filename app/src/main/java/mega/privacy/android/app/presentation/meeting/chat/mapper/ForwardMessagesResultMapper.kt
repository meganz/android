package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.ForwardMessagesToChatsResult
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import javax.inject.Inject

/**
 * Maps a list of [ForwardResult] to [ForwardMessagesToChatsResult]
 */
class ForwardMessagesResultMapper @Inject constructor() {

    /**
     * Invoke.
     */
    operator fun invoke(
        results: List<ForwardResult>,
        messagesCount: Int,
    ): ForwardMessagesToChatsResult {
        val successCount = results.count { it is ForwardResult.Success }
        val errorNotAvailableCount = results.count { it is ForwardResult.ErrorNotAvailable }
        val generalErrorCount = results.count { it is ForwardResult.GeneralError }
        val errorCount = errorNotAvailableCount + generalErrorCount
        val resultsCount = results.size
        val firstSuccess = results.firstOrNull { it is ForwardResult.Success }
            ?.let { it as ForwardResult.Success }?.chatId
        val chatId = firstSuccess?.let {
            if (results.filterIsInstance<ForwardResult.Success>()
                    .all { it.chatId == firstSuccess }
            ) {
                firstSuccess
            } else {
                null
            }
        }

        return when {
            successCount == resultsCount -> {
                ForwardMessagesToChatsResult.AllSucceeded(chatId, messagesCount)
            }

            errorNotAvailableCount == resultsCount -> {
                ForwardMessagesToChatsResult.AllNotAvailable(messagesCount)
            }

            errorCount == resultsCount -> {
                ForwardMessagesToChatsResult.AllFailed(messagesCount)
            }

            successCount + errorNotAvailableCount == resultsCount -> {
                ForwardMessagesToChatsResult.SomeNotAvailable(chatId, errorNotAvailableCount)
            }

            else -> {
                ForwardMessagesToChatsResult.SomeFailed(chatId, errorCount)
            }
        }
    }
}
