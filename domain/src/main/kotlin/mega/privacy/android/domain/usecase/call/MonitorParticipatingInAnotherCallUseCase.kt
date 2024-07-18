package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor participating in a call in other call use case
 */
class MonitorParticipatingInAnotherCallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getCallIdsOfOthersCallsUseCase: GetCallIdsOfOthersCallsUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
) {

    /**
     * Invoke
     * If the call status is Initial, Connecting, Joining, InProgress, it means that the user is participating in a call.
     * If the call status is UserNoPresent, Destroyed or TerminatingUserParticipation, we have to check other calls to see if the user is participating in any of them.
     *
     * @param currentChatId Current chat id, for filtering calls in other chats.
     * @return flow of List of [ChatCall] with active calls in other chats or empty otherwise.
     */
    operator fun invoke(currentChatId: Long): Flow<List<ChatCall>> {
        return monitorChatCallUpdatesUseCase()
            .map { getAnotherCalls(currentChatId) }
            .onStart { emit(getAnotherCalls(currentChatId)) }
    }

    private suspend fun getAnotherCalls(currentChatId: Long): List<ChatCall> =
        getCallIdsOfOthersCallsUseCase(currentChatId)
            .mapNotNull { getChatCallUseCase(it) }
}