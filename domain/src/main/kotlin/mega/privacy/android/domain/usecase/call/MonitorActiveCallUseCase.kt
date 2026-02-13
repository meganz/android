package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Use case to monitor if there is an active call, and return a list of the active calls
 */
class MonitorActiveCallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val callRepository: CallRepository,
) {
    operator fun invoke(): Flow<List<ChatCall>?> = monitorChatCallUpdatesUseCase()
        .filter { it.changes?.contains(ChatCallChanges.Status) == true }
        .map { chatCall ->
            if (chatCall.status != null && chatCall.status in activeStatuses) {
                listOf(chatCall)
            } else {
                getActiveCall()
            }
        }
        .onStart { emit(getActiveCall()) }
        .distinctUntilChanged()

    private suspend fun getActiveCall(): List<ChatCall>? =
        activeStatuses.map { status ->
            callRepository.getCallChatIdList(status)
                .mapNotNull { chatId ->
                    callRepository.getChatCall(chatId)
                }
        }.flatten()
            .takeUnless { it.isEmpty() }

    private val activeStatuses = listOf(
        ChatCallStatus.InProgress,
        ChatCallStatus.WaitingRoom,
        ChatCallStatus.Initial,
        ChatCallStatus.UserNoPresent,
        ChatCallStatus.Connecting,
        ChatCallStatus.Joining,
    )
}