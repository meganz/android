package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetCurrentChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor participating in a call in other chat use case
 *
 */
class MonitorParticipatingInACallInOtherChatUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getCurrentChatCallUseCase: GetCurrentChatCallUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
) {

    /**
     * Invoke
     * If the call status is Initial, Connecting, WaitingRoom, Joining, InProgress, it means that the user is participating in a call.
     * If the call status is Destroyed or TerminatingUserParticipation, we have to check other calls to see if the user is participating in any of them.
     *
     * @param currentChatId Current chat id, for filtering calls in other chats.
     * @return flow of [ChatCall]?, the call id if user is participating in any call, null if user is not participating in any calls
     */
    operator fun invoke(currentChatId: Long): Flow<ChatCall?> {
        return monitorChatCallUpdatesUseCase()
            .filter { it.chatId != currentChatId }
            .map { call ->
                when (call.status) {
                    ChatCallStatus.TerminatingUserParticipation,
                    ChatCallStatus.Destroyed,
                    ChatCallStatus.Unknown,
                    -> getChatCall(currentChatId)

                    else -> call
                }
            }
            .onStart { emit(getChatCall(currentChatId)) }
    }

    private suspend fun getChatCall(currentChatId: Long): ChatCall? =
        getCurrentChatCallUseCase()?.let { chatId ->
            if (currentChatId != chatId) {
                getChatCallUseCase(chatId)
            } else {
                null
            }
        }
}