package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.meeting.CallRecordingEvent
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.ChatSessionStatus
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
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
    operator fun invoke(chatId: Long) = monitorChatSessionUpdatesUseCase()
        .filter {
            chatId == it.chatId && it.session?.changes != null
                    && (it.session.hasChanged(ChatSessionChanges.SessionOnRecording)
                    || (it.session.hasChanged(ChatSessionChanges.Status) && it.session.status == ChatSessionStatus.Progress && it.session.isRecording))
        }.map {
            it.session?.let { session ->
                CallRecordingEvent(
                    isSessionOnRecording = session.isRecording,
                    participantRecording = getParticipantFullNameUseCase(session.peerId)
                )
            }
        }.onStart {
            getChatCallUseCase(chatId)?.sessionByClientId
                ?.filter { it.value.isRecording }
                ?.let {
                    if (it.values.isNotEmpty()) {
                        it.values.last().let { session ->
                            emit(
                                CallRecordingEvent(
                                    isSessionOnRecording = true,
                                    participantRecording = getParticipantFullNameUseCase(session.peerId)
                                )
                            )
                        }
                    }
                }
        }
}