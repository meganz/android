package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.filter
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.isCallFinished
import javax.inject.Inject

/**
 * Monitor when a call in a chat has been finish by 
 *  - Action of the user by hanging up
 *  - Call finished
 *  - Any other reason that makes the user not participating in the call
 */
class MonitorCallFinishedByChatIdUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(chatId: Long) = monitorChatCallUpdatesUseCase().filter {
        it.chatId == chatId && (it.status?.isCallFinished() == true
                || it.status == ChatCallStatus.TerminatingUserParticipation
                || it.status == ChatCallStatus.UserNoPresent)
    }.collect {
        broadcastCallRecordingConsentEventUseCase(null)
    }
}