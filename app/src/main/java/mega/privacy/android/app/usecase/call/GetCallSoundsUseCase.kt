package mega.privacy.android.app.usecase.call

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ErrorUtils.toThrowable

import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to control when a call-related sound should be played.
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 * @property getParticipantsChangesUseCase GetParticipantsChangesUseCase
 */
class GetCallSoundsUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase,
    private val getSessionStatusChangesUseCase: GetSessionStatusChangesUseCase,
    private val getCallStatusChangesUseCase: GetCallStatusChangesUseCase
) {

    companion object {
        const val SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION: Long = 10
        const val SECONDS_TO_WAIT_TO_WHEN_I_AM_ONLY_PARTICIPANT: Long = 2 * SECONDS_IN_MINUTE
    }

    /**
     * Participant info
     *
     * @property peerId     Peer ID of participant
     * @property clientId   Client ID of participant
     */
    data class ParticipantInfo(
        val peerId: Long,
        val clientId: Long
    )

    var countDownTimer: CustomCountDownTimer? = null
    val participants = ArrayList<ParticipantInfo>()

    /**
     * Method to get the appropriate sound
     *
     * @return CallSoundType
     */
    fun get(): Flowable<CallSoundType> =
        Flowable.create({ emitter ->
            val disposable = CompositeDisposable()

            getCallStatusChangesUseCase.getReconnectingStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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

            getSessionStatusChangesUseCase.getSessionChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { (call, sessionStatus, isRecoverable, peerId, clientId) ->
                        val participant =
                            ParticipantInfo(peerId = peerId, clientId = clientId)

                        if (call == null) {
                            stopCountDown(INVALID_HANDLE, participant)
                        } else {
                            megaChatApi.getChatRoom(call.chatid)?.let { chat ->
                                if (!chat.isGroup && !chat.isMeeting) {
                                    when (sessionStatus) {
                                        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                                            Timber.d("Session in progress")
                                            stopCountDown(call.chatid, participant)
                                        }
                                        MegaChatSession.SESSION_STATUS_DESTROYED -> {
                                            isRecoverable?.let { isRecoverableSession ->
                                                if (isRecoverableSession) {
                                                    Timber.d("Session destroyed, recoverable session. Wait 10 seconds to hang up")
                                                    emitter.startCountDown(
                                                        call,
                                                        participant,
                                                        SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                                    )

                                                } else {
                                                    Timber.d("Session destroyed, unrecoverable session.")
                                                    stopCountDown(
                                                        call.chatid,
                                                        participant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            getParticipantsChangesUseCase.checkIfIAmAloneOnACall()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { (chatId, onlyMeInTheCall) ->
                        if (onlyMeInTheCall) {
                            megaChatApi.getChatCall(chatId)?.let { call ->
                                if (call.hasLocalAudio()) {
                                    Timber.d("I am the only participant in the group call/meeting, muted micro")
                                    megaChatApi.disableAudio(call.chatid, null)
                                }
                            }

                            Timber.d("I am the only participant in the group call/meeting, wait 2 minutes to hang up")
                            MegaApplication.getChatManagement().startCounterToFinishCall(chatId,
                                SECONDS_TO_WAIT_TO_WHEN_I_AM_ONLY_PARTICIPANT)

                        } else {
                            MegaApplication.getChatManagement().stopCounterToFinishCall()
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            getCallStatusChangesUseCase.getCallStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { status ->
                        if (status == MegaChatCall.CALL_STATUS_DESTROYED) {
                            Timber.d("Call destroyed")
                            MegaApplication.getInstance()
                                .removeRTCAudioManager()
                            emitter.onNext(CallSoundType.CALL_ENDED)
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            getParticipantsChangesUseCase.getChangesFromParticipants()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { result ->
                        when (result.typeChange) {
                            TYPE_JOIN -> emitter.onNext(CallSoundType.PARTICIPANT_JOINED_CALL)
                            TYPE_LEFT -> emitter.onNext(CallSoundType.PARTICIPANT_LEFT_CALL)
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            emitter.setCancellable {
                disposable.clear()
            }

        }, BackpressureStrategy.LATEST)

    /**
     * Method to start the countdown to hang up the call
     *
     * @param call      MegaChatCall
     * @param seconds   Seconds to wait
     */
    private fun FlowableEmitter<CallSoundType>.startCountDown(
        call: MegaChatCall,
        participant: ParticipantInfo,
        seconds: Long
    ) {
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting && participants.contains(participant)) {
                if (countDownTimer == null) {
                    participants.remove(participant)

                    val countDownTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                    countDownTimer = CustomCountDownTimer(countDownTimerLiveData)
                    countDownTimerLiveData.observeOnce { counterState ->
                        counterState?.let { isFinished ->
                            if (isFinished) {
                                Timber.d("Count down timer ends. Hang call")
                                megaChatApi.hangChatCall(call.callId,
                                    OptionalMegaChatRequestListenerInterface(
                                        onRequestFinish = { _, error ->
                                            if (error.errorCode == MegaError.API_OK) {
                                                removeCountDownTimer()
                                            } else {
                                                this.onError(error.toThrowable())
                                            }
                                        }
                                    ))
                            }
                        }
                    }
                }

                Timber.d("Count down timer starts")
                countDownTimer?.start(seconds)
            }
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
                removeCountDownTimer()
            }
        }
    }

    /**
     * Remove Count down timer
     */
    private fun removeCountDownTimer() {
        countDownTimer?.apply {
            Timber.d("Count down timer stops")
            stop()
        }
        countDownTimer = null
    }
}