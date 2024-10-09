package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ParticipantsCountChange
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Am i alone on any call use case - to check if I am alone on any call and whether it is because I am waiting for others or because everyone has dropped out of the call.
 *
 * @property monitorChatCallUpdatesUseCase
 * @property chatRepository
 * @property callRepository
 */
class AmIAloneOnAnyCallUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
) {
    /**
     * Method to check if I am alone on any call and whether it is because I am waiting for others or because everyone has dropped out of the call.
     */
    operator fun invoke(): Flow<ParticipantsCountChange> =
        flow {
            emitAll(
                listOf(
                    callRepository.getCallHandleList(ChatCallStatus.Connecting),
                    callRepository.getCallHandleList(ChatCallStatus.Joining),
                    callRepository.getCallHandleList(ChatCallStatus.InProgress)
                ).flatten().mapNotNull {
                    callRepository.getChatCall(it)
                }.asFlow().map {
                    checkIfIAmAloneOnSpecificCall(it).apply {
                        isReceivedChange = false
                    }
                }
            )

            emitAll(
                monitorChatCallUpdatesUseCase()
                    .filter { it.changes != null }
                    .mapNotNull { call ->
                        when {
                            call.status == ChatCallStatus.InProgress || call.status == ChatCallStatus.Joining -> {
                                checkIfIAmAloneOnSpecificCall(call)
                            }

                            call.status != ChatCallStatus.Destroyed && call.status != ChatCallStatus.UserNoPresent && call.status != ChatCallStatus.TerminatingUserParticipation -> {
                                ParticipantsCountChange(
                                    chatId = call.chatId,
                                    callId = call.callId,
                                    onlyMeInTheCall = false,
                                    isReceivedChange = true
                                )
                            }

                            else -> null
                        }
                    }
            )
        }


    /**
     * Method to check if I am alone on a specific call and whether it is because I am waiting for others or because everyone has dropped out of the call
     *
     * @param call MegaChatCall
     * @return NumParticipantsChangesResult
     */
    private suspend fun checkIfIAmAloneOnSpecificCall(call: ChatCall): ParticipantsCountChange {
        var onlyMeInTheCall = false
        runCatching {
            chatRepository.getChatRoom(call.chatId)?.let { chat ->
                val isOneToOneCall = !chat.isGroup && !chat.isMeeting
                if (!isOneToOneCall) {
                    call.peerIdParticipants?.let { list ->
                        onlyMeInTheCall =
                            list.size == 1 && list[0] == chatRepository.getMyUserHandle()
                    }
                }
            }
        }

        return ParticipantsCountChange(
            chatId = call.chatId,
            callId = call.callId,
            onlyMeInTheCall = onlyMeInTheCall,
            isReceivedChange = true
        )
    }
}