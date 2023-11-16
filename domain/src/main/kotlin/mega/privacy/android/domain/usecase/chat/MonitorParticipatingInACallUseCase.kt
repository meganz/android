package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
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
     * @return flow of long?, the call id if user is participating in any call, null if user is not participating in any calls
     */
    operator fun invoke(): Flow<Long?> {
        return monitorChatCallUpdatesUseCase()
            .filter {
                monitoredStatuses.contains(it.status)
            }
            .distinctUntilChangedBy { it.status }
            .map {
                when (it.status) {
                    ChatCallStatus.Initial,
                    ChatCallStatus.WaitingRoom,
                    -> it.chatId

                    else -> getCurrentChatCallUseCase()
                }
            }
            .onStart { emit(getCurrentChatCallUseCase()) }
    }
}