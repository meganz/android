package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Use case to monitor if there is an active call.
 */
class MonitorHasActiveCallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val callRepository: CallRepository,
) {
    operator fun invoke() = monitorChatCallUpdatesUseCase()
        .filter { it.changes?.contains(ChatCallChanges.Status) == true }
        .map { it.status in activeStatuses || thereIsActiveCall() }
        .onStart { emit(thereIsActiveCall()) }
        .distinctUntilChanged()

    private suspend fun thereIsActiveCall() =
        activeStatuses.firstNotNullOfOrNull { status ->
            callRepository.getCallHandleList(status).takeIf { it.isNotEmpty() }
        } != null

    private val activeStatuses = listOf(
        ChatCallStatus.Initial,
        ChatCallStatus.UserNoPresent,
        ChatCallStatus.Connecting,
        ChatCallStatus.WaitingRoom,
        ChatCallStatus.Joining,
        ChatCallStatus.InProgress,
    )
}