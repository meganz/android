package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorRecordedChatsUseCase @Inject constructor(
    private val monitorActiveCallUseCase: MonitorActiveCallUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase,
) {

    operator fun invoke(): Flow<CallRecordingEvent> {
        return monitorActiveCallUseCase()
            .onEach {
                Log.d("MonitorRecordedChatsUseCase: Active Call List: $it")
            }
            .flatMapLatest { list ->
                mapToRecordingEventFlow(list)
                    ?: flowOf(CallRecordingEvent.NotRecording)
            }
    }

    fun recordingStatusChangedFlow(chatIDs: List<Long>) = monitorChatSessionUpdatesUseCase()
        .onEach {
            Log.d("MonitorRecordedChatsUseCase: Chat Session List for ids $chatIDs: $it")
        }
        .mapNotNull { result ->
            result.session?.let { session ->
                result.call?.let { call ->
                    session to call
                }
            }
        }
        .filter { (_, call) -> call.chatId in chatIDs }
        .filter { (session, _) -> isRecordingStatusChange(session) }
        .map { (session, call) ->
            if (session.hasChanged(ChatSessionChanges.Status)) {
                // I started participating in the call and it is being recorded
                CallRecordingEvent.PreexistingRecording(chatId = call.chatId)
            } else {
                // I'm participating in the call and started to be recorded or stopped being recorded
                if (session.isRecording) {
                    CallRecordingEvent.Recording(
                        chatId = call.chatId,
                        participantRecording = getParticipantFullNameUseCase(session.peerId)
                    )
                } else {
                    CallRecordingEvent.RecordingEnded(chatId = call.chatId)
                }
            }
        }

    private fun isRecordingStatusChange(chatSession: ChatSession): Boolean {
        return chatSession.hasChanged(ChatSessionChanges.SessionOnRecording) ||
                isRecordingActiveSession(chatSession)
    }

    private fun isRecordingActiveSession(session: ChatSession): Boolean =
        session.hasChanged(ChatSessionChanges.Status) &&
                session.status == ChatSessionStatus.Progress &&
                session.isRecording

    private fun mapToRecordingEventFlow(list: List<ChatCall>?) =
        list?.filter { it.status != null && it.status in onCallStatuses }
            ?.let { callList ->
                recordingStatusChangedFlow(callList.map { it.chatId })
                    .onStart {
                        callList
                            .forEach { chatCall ->
                                if (chatCall.sessionByClientId.values.any { it.isRecording }) {
                                    emit(CallRecordingEvent.PreexistingRecording(chatId = chatCall.chatId))
                                }
                            }
                    }
            }


    private val onCallStatuses = listOf(
        ChatCallStatus.InProgress,
        ChatCallStatus.Connecting,
        ChatCallStatus.Joining,
    )
}