package mega.privacy.android.app.usecase.call

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.rxFlowable
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.utils.Constants.SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ChatSessionTermCode
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainImmediateDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallsReconnectingStatusUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastWaitingForOtherParticipantsHasEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
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
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val amIAloneOnAnyCallUseCase: AmIAloneOnAnyCallUseCase,
    private val broadcastWaitingForOtherParticipantsHasEndedUseCase: BroadcastWaitingForOtherParticipantsHasEndedUseCase,
    @ApplicationScope private val sharingScope: CoroutineScope,
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

    var finishCallCountDownTimer: CustomCountDownTimer? = null
    var waitingForOthersCountDownTimer: CustomCountDownTimer? = null
    private var shouldPlaySoundWhenShowWaitingRoomDialog: Boolean = true

    val participants = ArrayList<ParticipantInfo>()
    val disposable = CompositeDisposable()

    /**
     * Method to get the appropriate sound
     *
     * @return CallSoundType
     */
    operator fun invoke() =
        Flowable.create({ emitter ->

            rxFlowable<Boolean> { monitorCallsReconnectingStatusUseCase() }
                .subscribeBy(
                    onNext = {
                        if (it) {
                            Timber.d("Call reconnecting")
                            emitter.onNext(CallSoundType.CALL_RECONNECTING)
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                ).addTo(disposable)

            sharingScope.launch {
                monitorChatSessionUpdatesUseCase().catch {
                    Timber.e(it.stackTraceToString())
                }.collect { sessionUpdate ->
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
                                            stopCountDown(chatId, participant)
                                        }

                                        ChatSessionStatus.Destroyed -> {
                                            (when (session.termCode) {
                                                ChatSessionTermCode.NonRecoverable -> false
                                                ChatSessionTermCode.Recoverable -> true
                                                else -> null
                                            })?.let { isRecoverableSession ->
                                                if (isRecoverableSession) {
                                                    Timber.d("Session destroyed, recoverable session. Wait 10 seconds to hang up")
                                                    startFinishCallCountDown(
                                                        chat,
                                                        callId,
                                                        participant,
                                                        SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                                    )

                                                } else {
                                                    Timber.d("Session destroyed, unrecoverable session.")
                                                    stopCountDown(
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
                            stopCountDown(INVALID_HANDLE, participant)
                        }

                    }
                }
            }

            amIAloneOnAnyCallUseCase()
                .asFlowable(sharingScope.coroutineContext)
                .subscribeBy(
                    onNext = { (chatId, onlyMeInTheCall, waitingForOthers) ->
                        removeWaitingForOthersCountDownTimer()
                        MegaApplication.getChatManagement().stopCounterToFinishCall()

                        if (onlyMeInTheCall) {
                            if (waitingForOthers) {
                                val liveD: MutableLiveData<Boolean> = MutableLiveData()
                                waitingForOthersCountDownTimer = CustomCountDownTimer(liveD)

                                liveD.observeOnce { counterState ->
                                    counterState?.let { isFinished ->
                                        if (isFinished) {
                                            MegaApplication.getChatManagement()
                                                .startCounterToFinishCall(chatId)

                                            broadcastWaitingForOtherParticipantsHasEnded(
                                                chatId
                                            )

                                            megaChatApi.getChatCall(chatId)?.let { call ->
                                                if (call.hasLocalAudio()) {
                                                    Timber.d("I am the only participant in the group call/meeting, muted micro")
                                                    megaChatApi.disableAudio(call.chatid, null)
                                                }
                                            }

                                            removeWaitingForOthersCountDownTimer()
                                        }
                                    }
                                }

                                waitingForOthersCountDownTimer?.start(
                                    SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL
                                )
                            } else {
                                MegaApplication.getChatManagement()
                                    .startCounterToFinishCall(chatId)
                            }
                        } else {
                            MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = false
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            sharingScope.launch {
                monitorChatCallUpdatesUseCase()
                    .collectLatest { call ->
                        withContext(mainImmediateDispatcher) {
                            call.changes?.apply {
                                Timber.d("Monitor chat call updated, changes $this")
                                if (contains(ChatCallChanges.Status)) {
                                    when (call.status) {
                                        ChatCallStatus.TerminatingUserParticipation -> {
                                            Timber.d("Terminating user participation")
                                            removeWaitingForOthersCountDownTimer()
                                            MegaApplication.getChatManagement()
                                                .stopCounterToFinishCall()
                                            rtcAudioManagerGateway.removeRTCAudioManager()
                                            emitter.onNext(CallSoundType.CALL_ENDED)
                                        }

                                        else -> {}
                                    }
                                }

                                if (contains(ChatCallChanges.WaitingRoomUsersEntered)) {
                                    if (call.waitingRoom?.peers?.size == 1) {
                                        shouldPlaySoundWhenShowWaitingRoomDialog = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            if (shouldPlaySoundWhenShowWaitingRoomDialog) {
                                                emitter.onNext(CallSoundType.WAITING_ROOM_USERS_ENTERED)
                                            }
                                        }, 1000)
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
            }

            getParticipantsChangesUseCase.getChangesFromParticipants()
                .subscribeBy(
                    onNext = { result ->
                        sharingScope.launch {
                            callsPreferencesGateway
                                .getCallsSoundNotificationsPreference()
                                .collectLatest { soundNotifications ->
                                    val isEnabled =
                                        soundNotifications == CallsSoundNotifications.Enabled
                                    if (isEnabled) {
                                        when (result.typeChange) {
                                            TYPE_JOIN -> emitter.onNext(CallSoundType.PARTICIPANT_JOINED_CALL)
                                            TYPE_LEFT -> emitter.onNext(CallSoundType.PARTICIPANT_LEFT_CALL)
                                        }
                                    }
                                    this.cancel()
                                }
                        }

                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            emitter.setCancellable {
                removeWaitingForOthersCountDownTimer()
                disposable.clear()
            }

        }, BackpressureStrategy.LATEST).asFlow()

    /**
     * Broadcasting that waiting for other participants has ended
     */
    private fun broadcastWaitingForOtherParticipantsHasEnded(chatId: Long) {
        sharingScope.launch {
            broadcastWaitingForOtherParticipantsHasEndedUseCase(chatId, false)
        }
    }

    /**
     * Hang call
     *
     * @param callId    Call id
     */
    private fun hangCall(callId: Long) {
        sharingScope.launch {
            runCatching {
                hangChatCallUseCase(callId)
            }.onSuccess {
                removeFinishCallCountDownTimer()
            }.onFailure {
                Timber.e(it.stackTraceToString())
            }
        }
    }

    /**
     * Method to start the countdown to hang up the call
     *
     */
    private suspend fun startFinishCallCountDown(
        chat: ChatRoom,
        callId: Long,
        participant: ParticipantInfo,
        seconds: Long,
    ) = withContext(mainImmediateDispatcher) {

        if (!chat.isGroup && !chat.isMeeting && participants.contains(participant)) {
            if (finishCallCountDownTimer == null) {
                participants.remove(participant)

                val countDownTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                finishCallCountDownTimer = CustomCountDownTimer(countDownTimerLiveData)

                countDownTimerLiveData.observeOnce { counterState ->
                    counterState?.let { isFinished ->
                        if (isFinished) {
                            Timber.d("Count down timer ends. Hang call")
                            hangCall(callId)
                        }
                    }
                }
            }

            Timber.d("Count down timer starts")
            finishCallCountDownTimer?.start(seconds)
        }
    }

    /**
     * Method to stop the countdown
     *
     * @param chatId        Chat ID
     * @param participant   ParticipantInfo
     */
    private fun stopCountDown(chatId: Long, participant: ParticipantInfo) {
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
                removeFinishCallCountDownTimer()
            }
        }
    }

    /**
     * Remove Finish call Count down timer
     */
    private fun removeFinishCallCountDownTimer() {
        finishCallCountDownTimer?.apply {
            Timber.d("Count down timer stops")
            stop()
        }
        finishCallCountDownTimer = null
    }

    /**
     * Remove Waiting for others Count down timer
     */
    private fun removeWaitingForOthersCountDownTimer() {
        waitingForOthersCountDownTimer?.apply {
            Timber.d("Count down timer stops")
            stop()
        }
        waitingForOthersCountDownTimer = null
    }
}
