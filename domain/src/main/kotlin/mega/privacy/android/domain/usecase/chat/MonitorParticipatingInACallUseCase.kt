package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import javax.inject.Inject

/**
 * Monitor Participating In A Call Use Case
 *
 */
class MonitorParticipatingInACallUseCase @Inject constructor(
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
) {
    /**
     * Invoke
     * If the call status is Initial or WaitingRoom, it means that the user is participating in a call.
     * If the call status is Destroyed or TerminatingUserParticipation, we have to check other calls to see if the user is participating in any of them.
     * @return flow of boolean, true if user is participating in any call, false if user is not participating in any calls
     */
    operator fun invoke() = monitorChatCallUpdates()
        .map { it.status }
        .distinctUntilChanged()
        .mapNotNull {
            when (it) {
                ChatCallStatus.InProgress,
                ChatCallStatus.WaitingRoom,
                -> true

                ChatCallStatus.Destroyed,
                ChatCallStatus.TerminatingUserParticipation,
                -> isParticipatingInChatCallUseCase()

                else -> null
            }
        }
        .onStart { emit(isParticipatingInChatCallUseCase()) }
}