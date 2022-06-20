package mega.privacy.android.app.usecase.call

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Main use case to get changes in participants
 */
class GetParticipantsChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    companion object {
        const val MAX_NUM_OF_WAITING_SHIFTS = 2
        const val NUM_OF_SECONDS_TO_WAIT: Long = 1
    }

    var joinedCountDownTimer: CustomCountDownTimer? = null
    var leftCountDownTimer: CustomCountDownTimer? = null

    var numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS
    var numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS

    val joinedParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val leftParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val peerIdsJoined = ArrayList<Long>()
    val peerIdsLeft = ArrayList<Long>()

    /**
     * Participants' changes result
     *
     * @property chatId        Chat ID of the call
     * @property typeChange    TYPE_JOIN or TYPE_LEFT
     * @property peers        List of user IDs
     */
    data class ParticipantsChangesResult(
        val chatId: Long?,
        val typeChange: Int,
        val peers: ArrayList<Long>?,
    )

    /**
     * Num participants changes result
     *
     * @property chatId        Chat ID of the call
     * @property onlyMeInTheCall    True, if I'm the only one in the call. False, if there are more participants.
     */
    data class NumParticipantsChangesResult(
        val chatId: Long?,
        val onlyMeInTheCall: Boolean,
    )

    /**
     * Method to check if I am alone in the meeting o group call
     */
    fun checkIfIAmAloneOnACall(): Flowable<NumParticipantsChangesResult> =
        Flowable.create({ emitter ->
            val callCompositionObserver = Observer<MegaChatCall> { call ->
                megaChatApi.getChatRoom(call.chatid)?.let { chat ->
                    val isRequestSent =
                        MegaApplication.getChatManagement().isRequestSent(call.callId)
                    val isOneToOneCall = !chat.isGroup && !chat.isMeeting
                    if (!isRequestSent && !isOneToOneCall) {
                        val onlyMeInTheCall =
                            call.numParticipants == 1 && call.peeridParticipants.get(0) == megaChatApi.myUserHandle
                        emitter.onNext(NumParticipantsChangesResult(call.chatid, onlyMeInTheCall))
                    }
                }
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

    /**
     * Control when participants join or leave and the appropriate sound should be played.
     *
     * @param call MegaChatCall
     */
    private fun FlowableEmitter<ParticipantsChangesResult>.checkParticipantsChanges(call: MegaChatCall) {
        if (call.status != MegaChatCall.CALL_STATUS_IN_PROGRESS || call.peeridCallCompositionChange == megaChatApi.myUserHandle || call.callCompositionChange == 0)
            return

        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            when (call.callCompositionChange) {
                TYPE_JOIN -> {
                    peerIdsJoined.add(call.peeridCallCompositionChange)
                    if (numberOfShiftsToWaitToJoin > 0) {
                        numberOfShiftsToWaitToJoin--

                        if (joinedCountDownTimer == null) {
                            joinedCountDownTimer = CustomCountDownTimer(joinedParticipantLiveData)
                            joinedCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        joinedCountDownTimer?.stop()
                                        numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS

                                        val listOfPeers = ArrayList<Long>()
                                        listOfPeers.addAll(peerIdsJoined)
                                        val result = ParticipantsChangesResult(
                                            chatId = call.chatid,
                                            typeChange = TYPE_JOIN,
                                            listOfPeers
                                        )
                                        this.onNext(result)
                                        peerIdsJoined.clear()
                                    }
                                }
                            }

                        } else {
                            joinedCountDownTimer?.stop()
                        }

                        joinedCountDownTimer?.start(NUM_OF_SECONDS_TO_WAIT)
                    }
                }
                TYPE_LEFT -> {
                    peerIdsLeft.add(call.peeridCallCompositionChange)
                    if (numberOfShiftsToWaitToLeft > 0) {
                        numberOfShiftsToWaitToLeft--

                        if (leftCountDownTimer == null) {
                            leftCountDownTimer = CustomCountDownTimer(leftParticipantLiveData)
                            leftCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        leftCountDownTimer?.stop()
                                        numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS

                                        val listOfPeers = ArrayList<Long>()
                                        listOfPeers.addAll(peerIdsLeft)
                                        val result = ParticipantsChangesResult(
                                            chatId = call.chatid,
                                            typeChange = TYPE_LEFT,
                                            listOfPeers
                                        )

                                        this.onNext(result)
                                        peerIdsLeft.clear()
                                    }
                                }
                            }
                        } else {
                            leftCountDownTimer?.stop()
                        }

                        leftCountDownTimer?.start(NUM_OF_SECONDS_TO_WAIT)
                    }
                }
            }
        }
    }

    /**
     * Cancel all count downs timer
     */
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