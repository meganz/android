package mega.privacy.android.app.usecase.call

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Main use case to get changes in participants
 */
class GetParticipantsChangesUseCase @Inject constructor(
        private val megaChatApi: MegaChatApiAndroid
) {

    companion object {
        const val MAX_NUM_OF_WAITING_SHIFTS = 2

        var joinedCountDownTimer: CustomCountDownTimer? = null
        var leftCountDownTimer: CustomCountDownTimer? = null

        val soundController = CallSoundsController()

        var numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS
        var numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS

        val joinedParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()
        val leftParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()

    }

    /**
     * Participants' changes result
     *
     * @property chatId        Chat ID of the call
     * @property typeChange    TYPE_JOIN or TYPE_LEFT
     * @property peers        List of user IDs
     */
    data class ParticipantsChangesResult(
            val chatId: Long,
            val typeChange: Int,
            val peers: ArrayList<Long>
    )

    /**
     * Method to get local audio changes
     *
     * @return Flowable containing True, if audio is enabled. False, if audio is disabled.
     */
    fun getChangesFromParticipants(): Flowable<ParticipantsChangesResult> =
            Flowable.create({ emitter ->
                val callCompositionObserver = Observer<MegaChatCall> { call ->
                    emitter.checkParticipantsChanges(call)
                }

                LiveEventBus.get(EventConstants.EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
                        .observeForever(callCompositionObserver)

                emitter.setCancellable {
                    removeCountDown()
                    LiveEventBus.get(
                            EventConstants.EVENT_CALL_COMPOSITION_CHANGE,
                            MegaChatCall::class.java
                    )
                            .removeObserver(callCompositionObserver)
                }
            }, BackpressureStrategy.LATEST)

    val leftParticipantsObserver = Observer<Boolean> { counterState ->
        counterState?.let { isFinished ->
            if (isFinished) {
                soundController.playSound(CallSoundType.PARTICIPANT_LEFT_CALL)
                numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS
            }
        }
    }

    /**
     * Control when participants join or leave and the appropriate sound should be played.
     *
     * @param call MegaChatCall
     */
    private fun FlowableEmitter<ParticipantsChangesResult>.checkParticipantsChanges(call: MegaChatCall) {
        if (call.status != MegaChatCall.CALL_STATUS_IN_PROGRESS || call.peeridCallCompositionChange == megaChatApi.myUserHandle || call.callCompositionChange == 0)
            return

        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isMeeting && !chat.isGroup) {
                return
            }

            when (call.callCompositionChange) {
                TYPE_JOIN -> {
                    if (numberOfShiftsToWaitToJoin > 0) {
                        numberOfShiftsToWaitToJoin--

                        if (joinedCountDownTimer == null) {
                            joinedCountDownTimer = CustomCountDownTimer(joinedParticipantLiveData)
                            joinedCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        joinedCountDownTimer?.stop()
                                        numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS
                                        val peers = ArrayList<Long>()
                                        peers.add(call.peeridCallCompositionChange)
                                        val result = ParticipantsChangesResult(
                                                chatId = call.chatid,
                                                typeChange = TYPE_JOIN,
                                                peers
                                        )

                                        this.onNext(result)
                                    }
                                }
                            }

                        } else {
                            joinedCountDownTimer?.stop()
                        }


                        joinedCountDownTimer?.start(1)
                    }
                }
                TYPE_LEFT -> {

                    if (numberOfShiftsToWaitToLeft > 0) {
                        numberOfShiftsToWaitToLeft--

                        if (leftCountDownTimer == null) {
                            leftCountDownTimer = CustomCountDownTimer(leftParticipantLiveData)
                            leftCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        leftCountDownTimer?.stop()
                                        numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS
                                        val peers = ArrayList<Long>()
                                        peers.add(call.peeridCallCompositionChange)
                                        val result = ParticipantsChangesResult(
                                                chatId = call.chatid,
                                                typeChange = TYPE_LEFT,
                                                peers
                                        )

                                        this.onNext(result)
                                    }
                                }
                            }
                        } else {
                            leftCountDownTimer?.stop()
                        }

                        leftCountDownTimer?.start(1)
                    }
                }
            }
        }
    }

    private fun removeCountDown() {
        joinedCountDownTimer?.let {
            cancelCountDown(it)
        }

        leftCountDownTimer?.let {
            cancelCountDown(it)
        }
    }

    /**
     * Cancel count down timer
     */
    private fun cancelCountDown(countDownTimer: CustomCountDownTimer) {
        countDownTimer.apply {
            stop()
        }
    }
}