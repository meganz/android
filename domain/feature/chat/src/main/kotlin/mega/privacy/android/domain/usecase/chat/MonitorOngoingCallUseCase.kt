package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor ongoing call use case
 *
 */
class MonitorOngoingCallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getChatCallInProgress: GetChatCallInProgress,
) {

    /**
     * Invoke
     *
     * @return flow of [ChatCall] with the call in progress or null otherwise.
     */
    operator fun invoke(): Flow<ChatCall?> {
        return monitorChatCallUpdatesUseCase()
            .map { getChatCallInProgress() }
            .onStart { emit(getChatCallInProgress()) }
    }
}