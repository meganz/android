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
    private val getSessionStatusChangesUseCase: GetSessionStatusChangesUseCase
) {

    companion object {
        const val SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION: Long = 30
    }

    var countDownTimer: CustomCountDownTimer? = null
    val calls = ArrayList<Long>()

    /**
     * Method to get the appropriate sound
     *
     * @return CallSoundType
     */
    fun get(): Flowable<CallSoundType> =
        Flowable.create({ emitter ->
            val disposable = CompositeDisposable()

            getSessionStatusChangesUseCase.getSessionChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { result ->

                        when (result.sessionStatus) {
                            MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                                result.call?.let { call ->
                                    stopCountDown(call)
                                }
                            }

                            MegaChatSession.SESSION_STATUS_DESTROYED -> {
                                result.isRecoverable?.let { isRecoverableSession ->
                                    if (result.call == null) {
                                        emitter.onNext(CallSoundType.CALL_ENDED)
                                    } else {
                                        if (isRecoverableSession) {
                                            result.lastParticipantPeerId?.let { peerId ->
                                                if (peerId == megaChatApi.myUserHandle) {
                                                    emitter.startCountDown(
                                                        result.call,
                                                        SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                                    )
                                                }
                                            }
                                        } else {
                                            stopCountDown(result.call)

                                            megaChatApi.getChatRoom(result.call.chatid)
                                                ?.let { chat ->
                                                    if (!chat.isGroup && !chat.isMeeting) {
                                                        emitter.onNext(CallSoundType.CALL_ENDED)
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
     * @param call MegaChatCall
     * @param seconds Seconds to wait
     */
    private fun FlowableEmitter<CallSoundType>.startCountDown(
        call: MegaChatCall,
        seconds: Long
    ) {
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting) {
                calls.add(call.callId)
                if (countDownTimer == null) {
                    val countDownTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                    countDownTimer = CustomCountDownTimer(countDownTimerLiveData)
                    countDownTimerLiveData.observeOnce { counterState ->
                        counterState?.let { isFinished ->
                            if (isFinished) {
                                stopCountDown(call)
                                megaChatApi.hangChatCall(call.callId,
                                    OptionalMegaChatRequestListenerInterface(
                                        onRequestFinish = { _, error ->
                                            if (error.errorCode == MegaError.API_OK) {
                                                calls.remove(call.callId)
                                                MegaApplication.getInstance()
                                                    .removeRTCAudioManager()
                                                this.onNext(CallSoundType.CALL_ENDED)
                                            } else {
                                                this.onError(error.toThrowable())
                                            }
                                        }
                                    ))
                            }
                        }
                    }
                }

                countDownTimer?.start(seconds)
            }
        }
    }

    /**
     * Method to stop the countdown
     *
     * @param call MegaChatCall
     */
    private fun stopCountDown(call: MegaChatCall) {
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting && countDownTimer != null && calls.contains(call.callId)) {
                countDownTimer?.stop()
                countDownTimer = null
                calls.remove(call.callId)
            }
        }
    }
}

