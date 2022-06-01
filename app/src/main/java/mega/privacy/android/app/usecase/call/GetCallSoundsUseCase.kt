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
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
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
        const val SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION: Long = 10
    }

    var countDownTimer: CustomCountDownTimer? = null
    val peerIdList = ArrayList<Long>()

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
                                    stopCountDown(call.chatid, result.lastParticipantPeerId)
                                }
                            }

                            MegaChatSession.SESSION_STATUS_DESTROYED -> {
                                result.isRecoverable?.let { isRecoverableSession ->
                                    if (result.call == null) {
                                        stopCountDown(INVALID_HANDLE, result.lastParticipantPeerId)

                                        emitter.onNext(CallSoundType.CALL_ENDED)

                                    } else if (isRecoverableSession) {
                                        emitter.startCountDown(
                                            result.call, result.lastParticipantPeerId,
                                            SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                        )

                                    } else {
                                        stopCountDown(
                                            result.call.chatid,
                                            result.lastParticipantPeerId
                                        )

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
        peerId: Long,
        seconds: Long
    ) {
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting) {
                peerIdList.add(peerId)
                if (countDownTimer == null) {
                    val countDownTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                    countDownTimer = CustomCountDownTimer(countDownTimerLiveData)
                    countDownTimerLiveData.observeOnce { counterState ->
                        counterState?.let { isFinished ->
                            if (isFinished) {
                                stopCountDown(chat.chatId, peerId)
                                megaChatApi.hangChatCall(call.callId,
                                    OptionalMegaChatRequestListenerInterface(
                                        onRequestFinish = { _, error ->
                                            if (error.errorCode == MegaError.API_OK) {
                                                peerIdList.remove(peerId)
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
     * @param chatId Chat ID
     * @param peerId User Handle
     */
    private fun stopCountDown(chatId: Long, peerId: Long) {
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            if (countDownTimer != null && peerIdList.contains(peerId)) {
                countDownTimer?.stop()
                countDownTimer = null
                peerIdList.remove(peerId)
            }
        } else {
            megaChatApi.getChatRoom(chatId)?.let { chat ->
                if (!chat.isGroup && !chat.isMeeting && countDownTimer != null && peerIdList.contains(
                        peerId
                    )
                ) {
                    countDownTimer?.stop()
                    countDownTimer = null
                    peerIdList.remove(peerId)
                }
            }
        }

    }
}

