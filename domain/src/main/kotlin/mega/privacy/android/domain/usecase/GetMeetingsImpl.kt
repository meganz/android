package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.entity.meeting.MeetingParticipantsResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingResult
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import javax.inject.Inject

/**
 * Get meetings use case implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GetMeetingsImpl @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getMeetingsRepository: GetMeetingsRepository,
    private val meetingRoomMapper: MeetingRoomMapper,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : GetMeetings {

    companion object {
        private const val DEBOUNCE_TIMEOUT_MS = 250L
    }

    override fun invoke(): Flow<List<MeetingRoomItem>> =
        flow {
            val mutex = Mutex()
            val meetings = mutableMapOf<Long, MeetingRoomItem>()

            emit(meetings.addChatRooms(mutex).sortMeetings())

            emitAll(
                merge(
                    meetings.addScheduledMeetings(mutex),
                    meetings.updateParticipants(mutex),
                    meetings.monitorMutedChats(mutex),
                    meetings.monitorChatCalls(mutex),
                    meetings.monitorChatItems(mutex),
                    meetings.monitorScheduledMeetings(mutex)
                )
                    .debounce(DEBOUNCE_TIMEOUT_MS) // Needed for backpressure
                    .mapLatest { it.sortMeetings() }
            )
        }

    private suspend fun MutableMap<Long, MeetingRoomItem>.addChatRooms(mutex: Mutex): List<MeetingRoomItem> {
        chatRepository.getMeetingChatRooms()?.let { chatRooms ->
            mutex.withLock {
                chatRooms.forEach { chatRoom ->
                    if (!chatRoom.isArchived) {
                        put(chatRoom.chatId, chatRoom.toMeetingRoomItem())
                    }
                }
            }
        }
        return values.toList()
    }

    private fun MutableMap<Long, MeetingRoomItem>.addScheduledMeetings(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        flow {
            values.toList().forEach { item ->
                getMeetingScheduleData(item.chatId)?.let { schedData ->
                    mutex.withLock {
                        get(item.chatId)?.let { currentItem ->
                            val newItem = currentItem.copy(
                                schedId = schedData.schedId,
                                scheduledStartTimestamp = schedData.scheduledStartTimestamp,
                                scheduledEndTimestamp = schedData.scheduledEndTimestamp,
                                isRecurringDaily = schedData.isRecurringDaily,
                                isRecurringWeekly = schedData.isRecurringWeekly,
                                isRecurringMonthly = schedData.isRecurringMonthly,
                                isPending = schedData.isPending,
                                scheduledMeetingStatus = schedData.scheduledMeetingStatus
                            )
                            if (currentItem != newItem) {
                                put(item.chatId, newItem)
                                emit(values.toList())
                            }
                        }
                    }
                }
            }
        }

    private fun MutableMap<Long, MeetingRoomItem>.updateParticipants(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        flow {
            values.toList().forEach { item ->
                getMeetingParticipants(item.chatId)?.let { participants ->
                    mutex.withLock {
                        get(item.chatId)?.let { currentItem ->
                            val newItem = currentItem.copy(
                                firstUserChar = participants.firstUserChar,
                                firstUserAvatar = participants.firstUserAvatar,
                                firstUserColor = participants.firstUserColor,
                                secondUserChar = participants.secondUserChar,
                                secondUserAvatar = participants.secondUserAvatar,
                                secondUserColor = participants.secondUserColor,
                            )
                            if (currentItem != newItem) {
                                put(item.chatId, newItem)
                                emit(values.toList())
                            }
                        }
                    }
                }
            }
        }

    private fun MutableMap<Long, MeetingRoomItem>.monitorMutedChats(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        chatRepository.monitorMutedChats()
            .map {
                values.toList().forEach { item ->
                    if (item.isMuted != !chatRepository.isChatNotifiable(item.chatId)) {
                        mutex.withLock {
                            get(item.chatId)?.let { currentItem ->
                                val newItem = currentItem.copy(
                                    isMuted = !currentItem.isMuted,
                                )
                                put(item.chatId, newItem)
                            }
                        }
                    }
                }
                values.toList()
            }

    private fun MutableMap<Long, MeetingRoomItem>.monitorChatCalls(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        monitorChatCallUpdates()
            .filter { containsKey(it.chatId) }
            .map { chatCall ->
                chatRepository.getCombinedChatRoom(chatCall.chatId)?.let { chatRoom ->
                    val meetingStatus =
                        getMeetingsRepository.getScheduledMeetingStatus(chatRoom.chatId)
                    mutex.withLock {
                        get(chatRoom.chatId)?.let { currentItem ->
                            val newItem = currentItem.copy(
                                highlight = chatRoom.unreadCount > 0 || chatRoom.isCallInProgress
                                        || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted,
                                lastTimestamp = chatRoom.lastTimestamp,
                                scheduledMeetingStatus = meetingStatus
                            )
                            if (currentItem != newItem) {
                                put(chatRoom.chatId, newItem)
                            }
                        }
                    }
                }
                values.toList()
            }

    private fun MutableMap<Long, MeetingRoomItem>.monitorChatItems(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        chatRepository.monitorChatListItemUpdates()
            .map { chatListItem ->
                if (chatListItem.isArchived ||
                    chatListItem.isDeleted ||
                    chatListItem.changes == ChatListItemChanges.Deleted ||
                    chatListItem.changes == ChatListItemChanges.Closed
                ) {
                    mutex.withLock { remove(chatListItem.chatId) }
                    return@map values.toList()
                }

                delay(500) // Required to wait for new SDK values

                val newItem = chatRepository.getCombinedChatRoom(chatListItem.chatId)
                    ?.takeIf(CombinedChatRoom::isMeeting)
                    ?.toMeetingRoomItem()
                    ?.let { item ->
                        getMeetingParticipants(item.chatId)?.let { participants ->
                            item.copy(
                                firstUserChar = participants.firstUserChar,
                                firstUserAvatar = participants.firstUserAvatar,
                                firstUserColor = participants.firstUserColor,
                                secondUserChar = participants.secondUserChar,
                                secondUserAvatar = participants.secondUserAvatar,
                                secondUserColor = participants.secondUserColor,
                            )
                        } ?: item
                    }
                    ?.let { item ->
                        getMeetingScheduleData(item.chatId)?.let { schedData ->
                            item.copy(
                                schedId = schedData.schedId,
                                scheduledStartTimestamp = schedData.scheduledStartTimestamp,
                                scheduledEndTimestamp = schedData.scheduledEndTimestamp,
                                isRecurringDaily = schedData.isRecurringDaily,
                                isRecurringWeekly = schedData.isRecurringWeekly,
                                isRecurringMonthly = schedData.isRecurringMonthly,
                                isPending = schedData.isPending,
                                scheduledMeetingStatus = schedData.scheduledMeetingStatus
                            )
                        } ?: item
                    }
                    ?: return@map values.toList()

                mutex.withLock {
                    if (newItem != get(chatListItem.chatId)) {
                        put(chatListItem.chatId, newItem)
                    } else {
                        // do nothing
                    }
                }
                values.toList()
            }

    private fun MutableMap<Long, MeetingRoomItem>.monitorScheduledMeetings(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        monitorScheduledMeetingUpdates()
            .filter { containsKey(it.chatId) }
            .map { schedMeeting ->
                if (schedMeeting.isCanceled) {
                    mutex.withLock { remove(schedMeeting.chatId) }
                    return@map values.toList()
                }

                getMeetingScheduleData(schedMeeting.chatId)?.let { schedData ->
                    mutex.withLock {
                        get(schedMeeting.chatId)?.let { currentItem ->
                            val newItem = currentItem.copy(
                                schedId = schedData.schedId,
                                scheduledStartTimestamp = schedData.scheduledStartTimestamp,
                                scheduledEndTimestamp = schedData.scheduledEndTimestamp,
                                isRecurringDaily = schedData.isRecurringDaily,
                                isRecurringWeekly = schedData.isRecurringWeekly,
                                isRecurringMonthly = schedData.isRecurringMonthly,
                                isPending = schedData.isPending,
                                scheduledMeetingStatus = schedData.scheduledMeetingStatus
                            )
                            if (currentItem != newItem) {
                                put(currentItem.chatId, newItem)
                            }
                        }
                    }
                }
                values.toList()
            }

    private suspend fun getMeetingParticipants(chatId: Long): MeetingParticipantsResult? =
        runCatching { getMeetingsRepository.getMeetingParticipants(chatId) }.getOrNull()

    private suspend fun getMeetingScheduleData(chatId: Long): ScheduledMeetingResult? =
        runCatching { getMeetingsRepository.getMeetingScheduleData(chatId) }.getOrNull()

    private suspend fun List<MeetingRoomItem>.sortMeetings(): List<MeetingRoomItem> =
        withContext(dispatcher) {
            sortedWith { firstMeeting, secondMeeting ->
                when {
                    firstMeeting.isPending && secondMeeting.isPending -> {
                        when {
                            firstMeeting.scheduledStartTimestamp!! > secondMeeting.scheduledStartTimestamp!! -> 1
                            firstMeeting.scheduledStartTimestamp < secondMeeting.scheduledStartTimestamp -> -1
                            else -> 0
                        }
                    }
                    !firstMeeting.isPending && !secondMeeting.isPending -> {
                        when {
                            firstMeeting.highlight && !secondMeeting.highlight -> -1
                            !firstMeeting.highlight && secondMeeting.highlight -> 1
                            firstMeeting.lastTimestamp > secondMeeting.lastTimestamp -> -1
                            firstMeeting.lastTimestamp < secondMeeting.lastTimestamp -> 1
                            else -> 0
                        }
                    }
                    firstMeeting.isPending && !secondMeeting.isPending -> -1
                    else -> 1
                }
            }
        }

    private suspend fun CombinedChatRoom.toMeetingRoomItem(): MeetingRoomItem =
        meetingRoomMapper.invoke(
            this,
            chatRepository::isChatNotifiable,
            chatRepository::isChatLastMessageGeolocation
        )
}
