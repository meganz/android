package mega.privacy.android.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Default get meetings use case implementation.
 */
class DefaultGetMeetings @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getMeetingsRepository: GetMeetingsRepository,
    private val meetingRoomMapper: MeetingRoomMapper,
) : GetMeetings {

    override fun invoke(mutex: Mutex): Flow<List<MeetingRoomItem>> =
        flow {
            val meetings = mutableListOf<MeetingRoomItem>()

            meetings.addChatRooms(mutex)
            emit(meetings)

            emitAll(
                merge(
                    meetings.addScheduledMeetings(mutex),
                    meetings.updateFields(mutex),
                    meetings.monitorMutedChats(mutex),
                    meetings.monitorChatCalls(mutex),
                    meetings.monitorChatItems(mutex),
                    meetings.monitorScheduledMeetings(mutex)
                )
            )
        }

    private suspend fun MutableList<MeetingRoomItem>.addChatRooms(mutex: Mutex) {
        chatRepository.getMeetingChatRooms()?.forEach { chatRoom ->
            if (!chatRoom.isArchived) {
                add(chatRoom.toMeetingRoomItem())
            }
        }
        sortMeetings(mutex)
    }

    private suspend fun MutableList<MeetingRoomItem>.updateFields(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        getMeetingsRepository.getUpdatedMeetingItems(this, mutex)

    private suspend fun MutableList<MeetingRoomItem>.addScheduledMeetings(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        flow {
            toList().forEach { item ->
                getScheduledMeetingItem(item)?.let { updatedItem ->
                    mutex.withLock {
                        val newIndex = indexOfFirst { updatedItem.chatId == it.chatId }
                        if (newIndex != -1) {
                            val newUpdatedItem = get(newIndex).copy(
                                schedId = updatedItem.schedId,
                                scheduledStartTimestamp = updatedItem.scheduledStartTimestamp,
                                scheduledEndTimestamp = updatedItem.scheduledEndTimestamp,
                                isRecurringDaily = updatedItem.isRecurringDaily,
                                isRecurringWeekly = updatedItem.isRecurringWeekly,
                                isRecurringMonthly = updatedItem.isRecurringMonthly,
                                isPending = updatedItem.isPending,
                            )
                            set(newIndex, newUpdatedItem)
                            emit(this@addScheduledMeetings)
                        }
                    }
                }
            }
            sortMeetings(mutex)
            emit(this@addScheduledMeetings)
        }

    private suspend fun MutableList<MeetingRoomItem>.monitorMutedChats(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorMutedChats()
            .map {
                apply {
                    val existingIndex =
                        indexOfFirst { it.isMuted != !chatRepository.isChatNotifiable(it.chatId) }
                    if (existingIndex != -1) {
                        val existingItem = get(existingIndex)
                        val updatedItem = existingItem.copy(
                            isMuted = !existingItem.isMuted,
                        )
                        mutex.withLock { set(existingIndex, updatedItem) }
                    }
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorChatCalls(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorChatCallUpdates()
            .filter { any { meeting -> meeting.chatId == it.chatId } }
            .map { chatCall ->
                apply {
                    val chatRoom =
                        chatRepository.getCombinedChatRoom(chatCall.chatId) ?: return@apply
                    val currentItemIndex = indexOfFirst { it.chatId == chatCall.chatId }
                    val currentItem = get(currentItemIndex)
                    val updatedItem = currentItem.copy(
                        highlight = chatRoom.unreadCount > 0 || chatRoom.isCallInProgress
                                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted,
                        lastTimestamp = chatRoom.lastTimestamp
                    )

                    if (currentItem != updatedItem) {
                        mutex.withLock { set(currentItemIndex, updatedItem) }
                        sortMeetings(mutex)
                    }
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorChatItems(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorChatListItemUpdates()
            .map { chatListItem ->
                apply {
                    val currentItemIndex = indexOfFirst { it.chatId == chatListItem.chatId }

                    if (chatListItem.isArchived ||
                        chatListItem.isDeleted ||
                        chatListItem.changes == ChatListItemChanges.Deleted ||
                        chatListItem.changes == ChatListItemChanges.Closed
                    ) {
                        if (currentItemIndex != -1) {
                            mutex.withLock { removeAt(currentItemIndex) }
                        }
                        return@apply
                    }

                    delay(500) // Required to wait for new SDK values
                    val newItem = chatRepository.getCombinedChatRoom(chatListItem.chatId)
                        ?.takeIf(CombinedChatRoom::isMeeting)
                        ?.toMeetingRoomItem()
                        ?.let { getMeetingsRepository.getUpdatedMeetingItem(it) }
                        ?: return@apply

                    val newUpdatedItem = getScheduledMeetingItem(newItem) ?: newItem
                    if (currentItemIndex != -1) {
                        val currentItem = get(currentItemIndex)
                        if (currentItem != newUpdatedItem) {
                            mutex.withLock { set(currentItemIndex, newUpdatedItem) }
                            sortMeetings(mutex)
                        }
                    } else {
                        mutex.withLock { add(newUpdatedItem) }
                        sortMeetings(mutex)
                    }
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorScheduledMeetings(mutex: Mutex): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorScheduledMeetingUpdates()
            .filter { any { meeting -> meeting.chatId == it.chatId } }
            .map { scheduledMeeting ->
                apply {
                    val currentItemIndex = indexOfFirst { it.chatId == scheduledMeeting.chatId }

                    if (scheduledMeeting.isCanceled) {
                        if (currentItemIndex != -1) {
                            mutex.withLock { removeAt(currentItemIndex) }
                        }
                        return@apply
                    }

                    val currentItem = get(currentItemIndex)
                    getScheduledMeetingItem(currentItem)?.let { updatedItem ->
                        mutex.withLock {
                            val newIndex = indexOfFirst { updatedItem.chatId == it.chatId }
                            if (newIndex != -1) {
                                val newUpdatedItem = currentItem.copy(
                                    schedId = updatedItem.schedId,
                                    scheduledStartTimestamp = updatedItem.scheduledStartTimestamp,
                                    scheduledEndTimestamp = updatedItem.scheduledEndTimestamp,
                                    isRecurringDaily = updatedItem.isRecurringDaily,
                                    isRecurringWeekly = updatedItem.isRecurringWeekly,
                                    isRecurringMonthly = updatedItem.isRecurringMonthly,
                                    isPending = updatedItem.isPending,
                                )
                                set(newIndex, newUpdatedItem)
                            }
                        }
                        sortMeetings(mutex)
                    }
                }
            }

    private suspend fun CombinedChatRoom.toMeetingRoomItem(): MeetingRoomItem =
        meetingRoomMapper.invoke(
            this,
            chatRepository::isChatNotifiable,
            chatRepository::isChatLastMessageGeolocation
        )

    private suspend fun getScheduledMeetingItem(item: MeetingRoomItem): MeetingRoomItem? =
        chatRepository.getScheduledMeetingsByChat(item.chatId)
            ?.firstOrNull()
            ?.takeIf { !it.isCanceled }
            ?.let { schedMeeting ->
                val isPending = item.isActive && schedMeeting.isPending()
                val isRecurringDaily = schedMeeting.rules?.freq == OccurrenceFrequencyType.Daily
                val isRecurringWeekly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Weekly
                val isRecurringMonthly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Monthly
                var startTimestamp = schedMeeting.startDateTime
                var endTimestamp = schedMeeting.endDateTime

                if (isPending && schedMeeting.rules != null) {
                    getNextOccurrence(item.chatId)?.let { nextOccurrence ->
                        startTimestamp = nextOccurrence.startDateTime
                        endTimestamp = nextOccurrence.endDateTime
                    } ?: return null
                }

                item.copy(
                    schedId = schedMeeting.schedId,
                    scheduledStartTimestamp = startTimestamp,
                    scheduledEndTimestamp = endTimestamp,
                    isRecurringDaily = isRecurringDaily,
                    isRecurringWeekly = isRecurringWeekly,
                    isRecurringMonthly = isRecurringMonthly,
                    isPending = isPending,
                )
            }

    private suspend fun MutableList<MeetingRoomItem>.sortMeetings(mutex: Mutex) {
        mutex.withLock {
            sortWith { firstMeeting, secondMeeting ->
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
    }

    private suspend fun getNextOccurrence(chatId: Long): ChatScheduledMeetingOccurr? =
        runCatching {
            val now = Instant.now().atZone(ZoneOffset.UTC)
            chatRepository.fetchScheduledMeetingOccurrencesByChat(
                chatId,
                now.minus(1L, ChronoUnit.HALF_DAYS).toEpochSecond()
            ).firstOrNull { occurr ->
                !occurr.isCancelled && occurr.parentSchedId == -1L &&
                        (occurr.startDateTime?.toZonedDateTime()?.isAfter(now) == true
                                || occurr.endDateTime?.toZonedDateTime()?.isAfter(now) == true)
            }
        }.getOrNull()

    private fun Long.toZonedDateTime(): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
}
