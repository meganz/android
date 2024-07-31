package mega.privacy.android.app.usecase.call

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainImmediateDispatcher
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to get changes in participants
 */
class GetParticipantsChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher,
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
        val chatId: Long,
        val typeChange: Int,
        val peers: ArrayList<Long>?,
    )

    /**
     * Method to get local audio changes
     *
     * @return Flowable containing True, if audio is enabled. False, if audio is disabled.
     */
    fun getChangesFromParticipants(): Flowable<ParticipantsChangesResult> =
        Flowable.create({ emitter ->
            sharingScope.launch {
                monitorChatCallUpdatesUseCase()
                    .catch { Timber.e(it) }
                    .collectLatest { call ->
                        withContext(mainImmediateDispatcher) {
                            call.changes?.apply {
                                Timber.d("Monitor chat call updated, changes $this")
                                if (contains(ChatCallChanges.CallComposition)) {
                                    megaChatApi.getChatRoom(call.chatId)?.let { chat ->
                                        if (chat.isGroup || chat.isMeeting)
                                            emitter.checkParticipantsChanges(call)
                                    }
                                }
                            }
                        }
                    }
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Control when participants join or leave and the appropriate sound should be played.
     *
     * @param call MegaChatCall
     */
    private fun FlowableEmitter<ParticipantsChangesResult>.checkParticipantsChanges(call: ChatCall) {
        if (call.status != ChatCallStatus.InProgress || call.peerIdCallCompositionChange == megaChatApi.myUserHandle || call.callCompositionChange == CallCompositionChanges.NoChange)
            return

        call.peerIdCallCompositionChange?.let { peerId ->
            when (call.callCompositionChange) {
                CallCompositionChanges.Added -> {
                    peerIdsJoined.add(peerId)
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
                                            chatId = call.chatId,
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

                CallCompositionChanges.Removed -> {
                    peerIdsLeft.add(peerId)
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
                                            chatId = call.chatId,
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

                else -> {}
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
