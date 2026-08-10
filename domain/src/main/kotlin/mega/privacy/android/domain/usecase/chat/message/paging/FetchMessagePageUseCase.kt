package mega.privacy.android.domain.usecase.chat.message.paging

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.usecase.chat.message.GetMessageListUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import javax.inject.Inject

/**
 * Fetch message page use case
 *
 * @property loadMessagesUseCase
 * @property getMessageListUseCase
 * @property monitorChatRoomMessagesUseCase
 */
class FetchMessagePageUseCase @Inject constructor(
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val getMessageListUseCase: GetMessageListUseCase,
    private val monitorChatRoomMessagesUseCase: MonitorChatRoomMessagesUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId
     * @return FetchMessagePageResponse
     */
    suspend operator fun invoke(
        chatId: Long,
    ): FetchMessagePageResponse {
        return kotlinx.coroutines.coroutineScope {
            val messageResponse =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching { getMessageListUseCase(monitorChatRoomMessagesUseCase(chatId)) }
                        .getOrElse {
                            if (it is TimeoutCancellationException) {
                                emptyList()
                            } else throw it
                        }
                }
            val loadResponse = loadMessagesUseCase(chatId)
            FetchMessagePageResponse(
                chatId = chatId,
                messages = messageResponse.await(),
                loadResponse = loadResponse,
            )
        }
    }
}
