package mega.privacy.android.app.usecase.call

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.utils.Constants.SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ChatSessionTermCode
import mega.privacy.android.domain.qualifier.MainImmediateDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.AmIAloneOnAnyCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallSoundEnabledUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallsReconnectingStatusUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastWaitingForOtherParticipantsHasEndedUseCase
import mega.privacy.android.domain.usecase.meeting.GetParticipantsChangesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Main use case to control when a call-related sound should be played.
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 * @property getParticipantsChangesUseCase GetParticipantsChangesUseCase
 */
class MonitorCallSoundsUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorCallsReconnectingStatusUseCase: MonitorCallsReconnectingStatusUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val monitorCallSoundEnabledUseCase: MonitorCallSoundEnabledUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val amIAloneOnAnyCallUseCase: AmIAloneOnAnyCallUseCase,
    private val broadcastWaitingForOtherParticipantsHasEndedUseCase: BroadcastWaitingForOtherParticipantsHasEndedUseCase,
    private val chatManagement: ChatManagement,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher,
) {

    companion object {
        const val SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION: Long = 10
        const val ONE_PARTICIPANT: Int = 1
    }

    /**
     * Participant info
     *
     * @property peerId     Peer ID of participant
     * @property clientId   Client ID of participant
     */
    data class ParticipantInfo(
        val peerId: Long,
        val clientId: Long,
    )

    private var shouldPlaySoundWhenShowWaitingRoomDialog: Boolean = true

    val participants = ArrayList<ParticipantInfo>()

    /**
     * Method to get the appropriate sound
     *
     * @return CallSoundType
     */
    operator fun invoke() = channelFlow<CallSoundType> {
        launch {
            monitorCallsReconnectingStatusUseCase().catch {
                Timber.e(it)
            }.collectLatest { isReconnecting ->
                if (isReconnecting) {
                    Timber.d("Call reconnecting")
                    send(CallSoundType.CALL_RECONNECTING)
                }
            }
        }

        launch {
            monitorChatSessionUpdatesUseCase().catch {
                Timber.e(it)
            }.collectLatest { sessionUpdate ->
                with(sessionUpdate) {
                    val session = session ?: return@with
                    val participant =
                        ParticipantInfo(peerId = session.peerId, clientId = session.clientId)
                    call?.apply {
                        getChatRoomUseCase(chatId)?.let { chat ->
                            if (!chat.isGroup && !chat.isMeeting) {
                                when (session.status) {
                                    ChatSessionStatus.Progress -> {
                                        Timber.d("Session in progress")
                                        checkParticipants(chatId, participant)
                                    }

                                    ChatSessionStatus.Destroyed -> {
                                        (when (session.termCode) {
                                            ChatSessionTermCode.NonRecoverable -> false
                                            ChatSessionTermCode.Recoverable -> true
                                            else -> null
                                        })?.let { isRecoverableSession ->
                                            if (isRecoverableSession) {
                                                Timber.d("Session destroyed, recoverable session. Wait 10 seconds to hang up")
                                                if (participants.contains(participant)) {
                                                    delay(
                                                        TimeUnit.SECONDS.toMillis(
                                                            SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                                        )
                                                    )
                                                    hangCall(callId)
                                                }
                                            } else {
                                                Timber.d("Session destroyed, unrecoverable session.")
                                                checkParticipants(
                                                    chatId,
                                                    participant
                                                )
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    } ?: run {
                        checkParticipants(INVALID_HANDLE, participant)
                    }

                }
            }
        }

        launch {
            amIAloneOnAnyCallUseCase()
                .collectLatest { (chatId, callId, onlyMeInTheCall) ->
                    withContext(mainImmediateDispatcher) {
                        MegaApplication.getChatManagement().stopCounterToFinishCall()
                    }
                    val waitingForOthers =
                        onlyMeInTheCall && chatManagement.isRequestSent(callId)
                    if (onlyMeInTheCall) {
                        if (waitingForOthers) {
                            delay(
                                TimeUnit.SECONDS.toMillis(
                                    SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL
                                )
                            )

                            withContext(mainImmediateDispatcher) {
                                MegaApplication.getChatManagement()
                                    .startCounterToFinishCall(chatId)
                            }

                            broadcastWaitingForOtherParticipantsHasEnded(
                                chatId
                            )

                            megaChatApi.getChatCall(chatId)?.let { call ->
                                if (call.hasLocalAudio()) {
                                    Timber.d("I am the only participant in the group call/meeting, muted micro")
                                    megaChatApi.disableAudio(call.chatid, null)
                                }
                            }
                        } else {
                            withContext(mainImmediateDispatcher) {
                                MegaApplication.getChatManagement()
                                    .startCounterToFinishCall(chatId)
                            }
                        }
                    } else {
                        MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = false
                    }
                }
        }

        launch {
            monitorChatCallUpdatesUseCase()
                .collectLatest { call ->
                    call.changes?.apply {
                        Timber.d("Monitor chat call updated, changes $this")
                        if (contains(ChatCallChanges.Status)) {
                            when (call.status) {
                                ChatCallStatus.TerminatingUserParticipation -> {
                                    Timber.d("Terminating user participation")
                                    MegaApplication.getChatManagement()
                                        .stopCounterToFinishCall()
                                    rtcAudioManagerGateway.removeRTCAudioManager()
                                    send(CallSoundType.CALL_ENDED)
                                }

                                else -> {}
                            }
                        }

                        if (contains(ChatCallChanges.WaitingRoomUsersEntered)) {
                            if (call.waitingRoom?.peers?.size == 1) {
                                shouldPlaySoundWhenShowWaitingRoomDialog = true
                                startWaitingRoomSound()
                            }
                        }

                        if (contains(ChatCallChanges.WaitingRoomUsersLeave)) {
                            shouldPlaySoundWhenShowWaitingRoomDialog = false
                        }

                        if (contains(ChatCallChanges.OutgoingRingingStop)) {
                            if (MegaApplication.getChatManagement()
                                    .isRequestSent(call.callId) && call.numParticipants == ONE_PARTICIPANT
                            ) {
                                hangCall(call.callId)
                            }
                        }
                    }
                }
        }

        launch {
            getParticipantsChangesUseCase()
                .collectLatest { result ->
                    val isEnabled = monitorCallSoundEnabledUseCase()
                        .firstOrNull() == CallsSoundEnabledState.Enabled

                    if (isEnabled) {
                        when (result.typeChange) {
                            TYPE_JOIN -> send(CallSoundType.PARTICIPANT_JOINED_CALL)
                            TYPE_LEFT -> send(CallSoundType.PARTICIPANT_LEFT_CALL)
                        }
                    }
                }
        }
    }

    /**
     * Broadcasting that waiting for other participants has ended
     */
    private fun CoroutineScope.broadcastWaitingForOtherParticipantsHasEnded(chatId: Long) {
        launch {
            broadcastWaitingForOtherParticipantsHasEndedUseCase(chatId, false)
        }
    }

    /**
     * Hang call
     *
     * @param callId    Call id
     */
    private fun CoroutineScope.hangCall(callId: Long) {
        launch {
            runCatching {
                hangChatCallUseCase(callId)
            }.onFailure {
                Timber.e(it.stackTraceToString())
            }
        }
    }

    private fun ProducerScope<CallSoundType>.startWaitingRoomSound() {
        this.launch {
            delay(1000)
            if (shouldPlaySoundWhenShowWaitingRoomDialog) {
                send(CallSoundType.WAITING_ROOM_USERS_ENTERED)
            }
        }
    }

    /**
     * Method to check the participants
     *
     * @param chatId        Chat ID
     * @param participant   ParticipantInfo
     */
    private fun checkParticipants(chatId: Long, participant: ParticipantInfo) {
        megaChatApi.getChatRoom(chatId)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting) {
                var participantToRemove: ParticipantInfo? = null
                participants.forEach { participantToCheck ->
                    if (participantToCheck.peerId == participant.peerId) {
                        participantToRemove = participantToCheck
                    }
                }

                if (participantToRemove != null) {
                    participants.remove(participantToRemove)
                }
                participants.add(participant)
            }
        }
    }
}
