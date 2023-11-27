package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
 * Monitor Participating In A Call Use Case
 *
 */
class MonitorParticipatingInACallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getCurrentChatCallUseCase: GetCurrentChatCallUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
) {
    private val monitoredStatuses = setOf(
        ChatCallStatus.Initial,
        ChatCallStatus.WaitingRoom,
        ChatCallStatus.TerminatingUserParticipation,
        ChatCallStatus.Destroyed,
    )

    /**
     * Invoke
     * If the call status is Initial or WaitingRoom, it means that the user is participating in a call.
     * If the call status is Destroyed or TerminatingUserParticipation, we have to check other calls to see if the user is participating in any of them.
     * @return flow of [ChatCall]?, the call id if user is participating in any call, null if user is not participating in any calls
     */
    operator fun invoke(): Flow<ChatCall?> {
        return monitorChatCallUpdatesUseCase()
            .filter { monitoredStatuses.contains(it.status) }
            .distinctUntilChangedBy { it.status }
            .map { call ->
                when (call.status) {
                    ChatCallStatus.Initial,
                    ChatCallStatus.WaitingRoom,
                    -> call

                    else -> getCurrentChatCallUseCase()?.let { getChatCallUseCase(it) }
                }
            }
            .onStart { emit(getCurrentChatCallUseCase()?.let { getChatCallUseCase(it) }) }
    }
}