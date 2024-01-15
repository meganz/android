package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetCurrentCallIdsInOtherChatsUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor participating in a call in other chat use case
 *
 */
class MonitorParticipatingInACallInOtherChatsUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getCurrentCallIdsInOtherChatsUseCase: GetCurrentCallIdsInOtherChatsUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
) {

    /**
     * Invoke
     * If the call status is Initial, Connecting, WaitingRoom, Joining, InProgress, it means that the user is participating in a call.
     * If the call status is Destroyed or TerminatingUserParticipation, we have to check other calls to see if the user is participating in any of them.
     *
     * @param currentChatId Current chat id, for filtering calls in other chats.
     * @return flow of List of [ChatCall] with active calls in other chats or empty otherwise.
     */
    operator fun invoke(currentChatId: Long): Flow<List<ChatCall>> {
        return monitorChatCallUpdatesUseCase()
            .map { getCallsInOtherChats(currentChatId) }
            .onStart { emit(getCallsInOtherChats(currentChatId)) }
    }

    private suspend fun getCallsInOtherChats(currentChatId: Long): List<ChatCall> =
        getCurrentCallIdsInOtherChatsUseCase(currentChatId)
            .mapNotNull { getChatCallUseCase(it) }
}