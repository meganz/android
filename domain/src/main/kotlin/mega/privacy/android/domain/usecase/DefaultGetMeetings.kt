package mega.privacy.android.domain.usecase

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import javax.inject.Inject

/**
 * Default get meetings use case implementation.
 */
@OptIn(FlowPreview::class)
class DefaultGetMeetings @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getMeetingsRepository: GetMeetingsRepository,
    private val meetingRoomMapper: MeetingRoomMapper,
) : GetMeetings {

    companion object {
        private const val DEBOUNCE_TIMEOUT_MS = 150L // Needed for backpressure
    }

    private val mutex = Mutex()

    override fun invoke(): Flow<List<MeetingRoomItem>> =
        flow {
            val meetings = mutableListOf<MeetingRoomItem>()

            meetings.addChatRooms()
            emit(meetings)

            emitAll(
                merge(
                    meetings.addScheduledMeetings(),
                    meetings.updateFields(),
                    meetings.monitorMutedChats(),
                    meetings.monitorChatCalls(),
                    meetings.monitorChatItems(),
                    meetings.monitorScheduledMeetings()
                ).debounce(DEBOUNCE_TIMEOUT_MS)
            )
        }

    private suspend fun MutableList<MeetingRoomItem>.addChatRooms() {
        chatRepository.getMeetingChatRooms()?.forEach { chatRoom ->
            if (!chatRoom.isArchived) {
                add(chatRoom.toMeetingRoomItem())
            }
        }
        sortMeetings()
    }

    private suspend fun MutableList<MeetingRoomItem>.updateFields(): Flow<MutableList<MeetingRoomItem>> =
        getMeetingsRepository.getUpdatedMeetingItems(this, mutex)

    private suspend fun MutableList<MeetingRoomItem>.addScheduledMeetings(): Flow<MutableList<MeetingRoomItem>> =
        flow {
            val iterator = listIterator()
            while (iterator.hasNext()) {
                mutex.withLock {
                    val item = iterator.next()
                    val schedMeetings = chatRepository.getScheduledMeetingsByChat(item.chatId)
                    if (!schedMeetings.isNullOrEmpty()) {
                        val schedMeeting = schedMeetings.first()
                        val updatedItem = item.copy(
                            schedId = schedMeeting.schedId,
                            isRecurring = schedMeetings.size > 1,
                            scheduledStartTimestamp = schedMeeting.startDateTime?.toEpochSecond(),
                            scheduledEndTimestamp = schedMeeting.endDateTime?.toEpochSecond(),
                        )
                        iterator.set(updatedItem)
                        emit(this@addScheduledMeetings)
                    }
                }
            }

            sortMeetings()
            emit(this@addScheduledMeetings)
        }

    private suspend fun MutableList<MeetingRoomItem>.monitorMutedChats(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorMutedChats()
            .map {
                apply {
                    val existingIndex = indexOfFirst { it.isMuted != !chatRepository.isChatNotifiable(it.chatId) }
                    if (existingIndex != -1) {
                        val existingItem = get(existingIndex)
                        val updatedItem = existingItem.copy(
                            isMuted = !existingItem.isMuted,
                        )
                        mutex.withLock { set(existingIndex, updatedItem) }
                    }
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorChatCalls(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorChatCallUpdates()
            .filter { any { meeting -> meeting.chatId == it.chatId } }
            .map { chatCall ->
                apply {
                    val chatRoom = chatRepository.getCombinedChatRoom(chatCall.chatId) ?: return@apply
                    val currentItemIndex = indexOfFirst { it.chatId == chatCall.chatId }
                    val updatedItem = get(currentItemIndex).copy(
                        highlight = chatRoom.unreadCount > 0 || chatRoom.isCallInProgress
                                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted,
                        lastTimestamp = chatRoom.lastTimestamp
                    )

                    mutex.withLock { set(currentItemIndex, updatedItem) }
                    sortMeetings()
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorChatItems(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorChatListItemUpdates()
            .map { chatListItem ->
                apply {
                    val currentItemIndex = indexOfFirst { it.chatId == chatListItem.chatId }

                    if (currentItemIndex != -1 && chatListItem.isArchived) {
                        mutex.withLock { removeAt(currentItemIndex) }
                        return@apply
                    }

                    val updated = chatRepository.getCombinedChatRoom(chatListItem.chatId)
                        ?.toMeetingRoomItem()
                        ?.let { getMeetingsRepository.getUpdatedMeetingItem(it) }
                        ?: return@apply

                    if (currentItemIndex != -1) {
                        val currentItem = get(currentItemIndex)
                        val updatedItem = updated.copy(
                            schedId = currentItem.schedId,
                            scheduledStartTimestamp = currentItem.scheduledStartTimestamp,
                            scheduledEndTimestamp = currentItem.scheduledEndTimestamp,
                        )
                        mutex.withLock { set(currentItemIndex, updatedItem) }
                    } else {
                        mutex.withLock { add(updated) }
                    }
                    sortMeetings()
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorScheduledMeetings(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorScheduledMeetingsUpdates()
            .filter { any { meeting -> meeting.chatId == it.chatId } }
            .map { scheduledMeeting ->
                apply {
                    val currentItemIndex = indexOfFirst { it.chatId == scheduledMeeting.chatId }
                    val updatedItem = get(currentItemIndex).copy(
                        schedId = scheduledMeeting.schedId,
                        scheduledStartTimestamp = scheduledMeeting.startDateTime?.toEpochSecond(),
                        scheduledEndTimestamp = scheduledMeeting.endDateTime?.toEpochSecond(),
                    )
                    mutex.withLock { set(currentItemIndex, updatedItem) }
                    sortMeetings()
                }
            }

    private suspend fun isRecurringScheduleMeeting(chatId: Long): Boolean =
        runCatching { chatRepository.fetchScheduledMeetingOccurrencesByChat(chatId) }
            .fold(
                onSuccess = { occurrences -> (occurrences?.size ?: 0) > 1 },
                onFailure = { false }
            )

    private suspend fun CombinedChatRoom.toMeetingRoomItem(): MeetingRoomItem =
        meetingRoomMapper.invoke(this,
            chatRepository::isChatNotifiable,
            chatRepository::isChatLastMessageGeolocation
        )

    private suspend fun MutableList<MeetingRoomItem>.sortMeetings() {
        mutex.withLock {
            sortWith(compareByDescending<MeetingRoomItem> { it.isScheduledMeeting() }
                .thenBy { it.scheduledStartTimestamp }
                .thenByDescending { it.lastTimestamp }
            )
        }
    }
}
