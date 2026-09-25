package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import javax.inject.Inject


/**
 * Main use case to get changes in participants
 */
class GetParticipantsChangesUseCase @Inject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) {

    companion object {
        /**
         * Maximum number of shifts to wait for a participant to join
         */
        private const val MAX_NUM_OF_WAITING_SHIFTS = 2

        /**
         * Number of seconds to wait before considering that a participant has joined/left
         */
        private const val NUM_OF_SECONDS_TO_WAIT: Long = 1

        internal const val TYPE_LEFT: Int = -1

        internal const val TYPE_JOIN: Int = 1
    }

    private var joinedCountDownTimerMap: MutableMap<Long, Job> = mutableMapOf()
    private var leftCountDownTimerMap: MutableMap<Long, Job> = mutableMapOf()

    private val numberOfShiftsToWaitToJoinMap = mutableMapOf<Long, Int>()
    private val numberOfShiftsToWaitToLeftMap = mutableMapOf<Long, Int>()

    private val joinedParticipantFlow = MutableSharedFlow<Pair<Long, Boolean>?>()
    private val leftParticipantFlow = MutableSharedFlow<Pair<Long, Boolean>?>()
    private val peerIdsJoined = mutableMapOf<Long, ArrayList<Long>>()
    private val peerIdsLeft = mutableMapOf<Long, ArrayList<Long>>()

    private lateinit var coroutineScope: CoroutineScope

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
     * Method to get changes from participants
     *
     * @return Flow containing ParticipantsChangesResult
     */
    operator fun invoke(): Flow<ParticipantsChangesResult> = channelFlow {
        coroutineScope = this
        launch {
            joinedParticipantFlow.collect {
                it?.let {
                    if (it.second) {
                        val listOfPeers = ArrayList<Long>()
                        peerIdsJoined[it.first]?.let { it1 -> listOfPeers.addAll(it1) }
                        numberOfShiftsToWaitToJoinMap[it.first] = MAX_NUM_OF_WAITING_SHIFTS
                        peerIdsJoined[it.first]?.clear()
                        send(
                            ParticipantsChangesResult(
                                it.first,
                                TYPE_JOIN,
                                listOfPeers
                            )
                        )
                    }
                }
            }
        }

        launch {
            leftParticipantFlow.collect {
                it?.let {
                    if (it.second) {
                        val listOfPeers = ArrayList<Long>()
                        peerIdsLeft[it.first]?.let { it1 -> listOfPeers.addAll(it1) }
                        numberOfShiftsToWaitToLeftMap[it.first] = MAX_NUM_OF_WAITING_SHIFTS
                        peerIdsLeft[it.first]?.clear()
                        send(
                            ParticipantsChangesResult(
                                it.first,
                                TYPE_LEFT,
                                listOfPeers
                            )
                        )
                    }
                }
            }
        }

        launch {
            monitorChatCallUpdatesUseCase()
                .collect { call ->
                    call.changes?.apply {
                        if (contains(ChatCallChanges.CallComposition)) {
                            getChatRoomUseCase(call.chatId)?.let { chat ->
                                if (chat.isGroup || chat.isMeeting) {
                                    val myHandle = getMyUserHandleUseCase()
                                    checkParticipantsChanges(call, myHandle)
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Control when participants join or leave and the appropriate sound should be played.
     *
     * @param call MegaChatCall
     * @return ParticipantsChangesResult or null if no changes
     */
    private fun checkParticipantsChanges(call: ChatCall, myHandle: Long) {
        if (call.status != ChatCallStatus.InProgress || call.peerIdCallCompositionChange == myHandle || call.callCompositionChange == CallCompositionChanges.NoChange)
            return

        call.peerIdCallCompositionChange?.let { peerId ->
            when (call.callCompositionChange) {
                CallCompositionChanges.Added -> {
                    val joinedList = peerIdsJoined.getOrDefault(call.chatId, null)
                    if (joinedList == null) {
                        peerIdsJoined[call.chatId] = arrayListOf(peerId)
                    } else {
                        joinedList.add(peerId)
                    }
                    val numberOfShiftsToWaitToJoin = numberOfShiftsToWaitToJoinMap.getOrDefault(
                        call.chatId,
                        MAX_NUM_OF_WAITING_SHIFTS
                    )

                    if (numberOfShiftsToWaitToJoin > 0) {
                        numberOfShiftsToWaitToJoinMap[call.chatId] = numberOfShiftsToWaitToJoin - 1
                        handleJoinedParticipants(call)
                    }
                }

                CallCompositionChanges.Removed -> {
                    val leftList = peerIdsLeft.getOrDefault(call.chatId, null)
                    if (leftList == null) {
                        peerIdsLeft[call.chatId] = arrayListOf(peerId)
                    } else {
                        leftList.add(peerId)
                    }
                    val numberOfShiftsToWaitToLeft = numberOfShiftsToWaitToLeftMap.getOrDefault(
                        call.chatId,
                        MAX_NUM_OF_WAITING_SHIFTS
                    )
                    if (numberOfShiftsToWaitToLeft > 0) {
                        numberOfShiftsToWaitToLeftMap[call.chatId] = numberOfShiftsToWaitToLeft - 1
                        handleLeftParticipants(call)
                    }
                }

                else -> {}
            }
        }
    }

    private fun handleJoinedParticipants(call: ChatCall) {
        val job = joinedCountDownTimerMap.getOrDefault(call.chatId, null)
        if (job == null || job.isActive.not()) {
            CustomCountDownTimer(
                joinedParticipantFlow, call.chatId, coroutineScope
            ).apply {
                val newJob = start(NUM_OF_SECONDS_TO_WAIT)
                joinedCountDownTimerMap[call.chatId] = newJob
            }
        }
    }

    private fun handleLeftParticipants(call: ChatCall) {
        val job = leftCountDownTimerMap.getOrDefault(call.chatId, null)
        if (job == null || job.isActive.not()) {
            CustomCountDownTimer(
                leftParticipantFlow, call.chatId, coroutineScope
            ).apply {
                val newJob = start(NUM_OF_SECONDS_TO_WAIT)
                leftCountDownTimerMap[call.chatId] = newJob
            }
        }
    }
}

private class CustomCountDownTimer(
    private val stateFlow: MutableSharedFlow<Pair<Long, Boolean>?>,
    private val chatId: Long,
    private val sharingScope: CoroutineScope,
) {

    fun start(seconds: Long): Job {
        return sharingScope.launch {
            delay(seconds * 1000)
            stateFlow.emit(Pair(chatId, true))
        }
    }
}
