package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import javax.inject.Inject

/**
 * Use case to monitor call session on recording.
 */
class MonitorCallSessionOnRecordingUseCase @Inject constructor(
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase,
) {

    /**
     * Invoke.
     */
    operator fun invoke(chatId: Long): Flow<CallRecordingEvent> = monitorChatSessionUpdatesUseCase()
        .filter {
            chatId == it.call?.chatId && it.session?.changes != null
                    && (it.session.hasChanged(ChatSessionChanges.SessionOnRecording)
                    || (it.session.hasChanged(ChatSessionChanges.Status) && it.session.status == ChatSessionStatus.Progress && it.session.isRecording))
        }.map {
            it.session?.let { session ->
                if (session.hasChanged(ChatSessionChanges.Status)) {
                    // I started participating in the call and it is being recorded
                    CallRecordingEvent.PreexistingRecording(chatId = chatId)
                } else {
                    // I'm participating in the call and started to be recorded or stopped being recorded
                    if (session.isRecording) {
                        CallRecordingEvent.Recording(
                            chatId = chatId,
                            participantRecording = getParticipantFullNameUseCase(session.peerId)
                        )
                    } else {
                        CallRecordingEvent.NotRecording
                    }
                }
            } ?: CallRecordingEvent.NotRecording
        }.onStart {
            getChatCallUseCase(chatId)?.sessionByClientId
                ?.filter { it.value.isRecording }
                ?.let {
                    if (it.values.isNotEmpty()) {
                        // I'm participating in the call and it is being recorded
                        emit(
                            CallRecordingEvent.PreexistingRecording(
                                chatId = chatId,
                            )
                        )
                    }
                }
        }
}