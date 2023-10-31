package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import javax.inject.Inject

/**
 * Monitor A Call In This Chat Use Case
 *
 */
class MonitorACallInThisChatUseCase @Inject constructor(
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val hasACallInThisChatByChatIdUseCase: HasACallInThisChatByChatIdUseCase,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke(chatId: Long) = monitorChatCallUpdates()
        .filter { it.chatId == chatId && (it.status == ChatCallStatus.Initial || it.status == ChatCallStatus.Destroyed) }
        .map { it.status == ChatCallStatus.Initial }
        .onStart { emit(hasACallInThisChatByChatIdUseCase(chatId)) }
}