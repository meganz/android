package mega.privacy.android.app.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ParticipantsCountChange
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Am i alone on any call use case - to check if I am alone on any call and whether it is because I am waiting for others or because everyone has dropped out of the call.
 *
 * @property getCallUseCase
 * @property monitorChatCallUpdatesUseCase
 * @property chatManagement
 * @property ChatRepository
 */
class AmIAloneOnAnyCallUseCase @Inject constructor(
    private val getCallUseCase: GetCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val chatManagement: ChatManagement,
    private val chatRepository: ChatRepository,
) {
    /**
     * Method to check if I am alone on any call and whether it is because I am waiting for others or because everyone has dropped out of the call.
     */
    operator fun invoke(): Flow<ParticipantsCountChange> =
        flow {
            emitAll(
                getCallUseCase.getCallsInProgressAndOnHold()
                    .asFlow()
                    .map {
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
                                    call.chatId,
                                    onlyMeInTheCall = false,
                                    waitingForOthers = false,
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
        var waitingForOthers = false
        var onlyMeInTheCall = false
        runCatching {
            chatRepository.getChatRoom(call.chatId)?.let { chat ->
                val isOneToOneCall = !chat.isGroup && !chat.isMeeting
                if (!isOneToOneCall) {
                    call.peerIdParticipants?.let { list ->
                        onlyMeInTheCall =
                            list.size == 1 && list[0] == chatRepository.getMyUserHandle()

                        waitingForOthers = onlyMeInTheCall &&
                                chatManagement.isRequestSent(call.callId)
                    }
                }
            }
        }

        return ParticipantsCountChange(
            call.chatId,
            onlyMeInTheCall,
            waitingForOthers,
            isReceivedChange = true
        )
    }

    private suspend fun checkIfIAmAloneOnSpecificCall(call: MegaChatCall): ParticipantsCountChange {
        var waitingForOthers = false
        var onlyMeInTheCall = false
        chatRepository.getChatRoom(call.chatid)?.let { chat ->
            val isOneToOneCall = !chat.isGroup && !chat.isMeeting
            if (!isOneToOneCall) {
                call.peeridParticipants?.let { list ->
                    onlyMeInTheCall =
                        list.size().toInt() == 1 && list.get(0) == chatRepository.getMyUserHandle()

                    waitingForOthers = onlyMeInTheCall &&
                            chatManagement.isRequestSent(call.callId)
                }
            }
        }

        return ParticipantsCountChange(
            call.chatid,
            onlyMeInTheCall,
            waitingForOthers,
            isReceivedChange = true
        )
    }
}