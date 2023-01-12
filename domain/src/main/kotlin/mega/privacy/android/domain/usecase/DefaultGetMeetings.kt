package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.qualifier.DefaultDispatcher
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
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : GetMeetings {

    companion object {
        private const val DEBOUNCE_TIMEOUT_MS = 250L // Needed for backpressure
    }

    override fun invoke(): Flow<List<MeetingRoomItem>> =
        flow {
            val meetings = mutableListOf<MeetingRoomItem>()

            meetings.addChatRooms()
            emit(meetings)

            meetings.updateFields()
            emit(meetings)

            if (meetings.addScheduledMeetings()) {
                emit(meetings)
            }

            emitAll(
                merge(
                    meetings.monitorMutedChats(),
                    meetings.monitorChatCalls(),
                    meetings.monitorChatItems(),
                    meetings.monitorScheduledMeetings()
                ).debounce(DEBOUNCE_TIMEOUT_MS)
            )
        }.flowOn(dispatcher)

    private suspend fun MutableList<MeetingRoomItem>.addChatRooms() {
        chatRepository.getMeetingChatRooms()?.forEach { chatRoom ->
            if (!chatRoom.isArchived) {
                add(chatRoom.toMeetingRoomItem())
            }
        }
        sortMeetings()
    }

    private suspend fun MutableList<MeetingRoomItem>.updateFields() =
        getMeetingsRepository.updateMeetingFields(this)

    private suspend fun MutableList<MeetingRoomItem>.addScheduledMeetings(): Boolean {
        val scheduledMeetings = chatRepository.getAllScheduledMeetings()
        scheduledMeetings?.forEach { scheduledMeeting ->
            val currentItemIndex = indexOfFirst { scheduledMeeting.chatId == it.chatId }
            if (currentItemIndex != -1) {
                val isRecurring = isRecurringScheduleMeeting(scheduledMeeting.chatId)
                val updatedMeeting = get(currentItemIndex).copy(
                    schedId = scheduledMeeting.schedId,
                    isRecurring = isRecurring,
                    scheduledStartTimestamp = scheduledMeeting.startDateTime?.toEpochSecond(),
                    scheduledEndTimestamp = scheduledMeeting.endDateTime?.toEpochSecond(),
                )
                set(currentItemIndex, updatedMeeting)
            }
        }

        return if (!scheduledMeetings.isNullOrEmpty()) {
            sortMeetings()
            true
        } else {
            false
        }
    }

    private suspend fun MutableList<MeetingRoomItem>.monitorMutedChats(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorMutedChats()
            .map {
                apply {
                    val existingIndex = indexOfFirst { it.isMuted != !chatRepository.isChatNotifiable(it.chatId) }
                    if (existingIndex != -1) {
                        val existingItem = get(existingIndex)
                        set(existingIndex, existingItem.copy(
                            isMuted = !existingItem.isMuted,
                        ))
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
                    set(currentItemIndex, updatedItem)
                    sortMeetings()
                }
            }

    private suspend fun MutableList<MeetingRoomItem>.monitorChatItems(): Flow<MutableList<MeetingRoomItem>> =
        chatRepository.monitorChatListItemUpdates()
            .map { chatListItem ->
                apply {
                    val currentItemIndex = indexOfFirst { it.chatId == chatListItem.chatId }

                    if (currentItemIndex != -1 && chatListItem.isArchived) {
                        removeAt(currentItemIndex)
                        return@apply
                    }

                    val updated = chatRepository.getCombinedChatRoom(chatListItem.chatId)
                        ?.toMeetingRoomItem()
                        ?.let { mutableListOf(it).apply { updateFields() }.first() }
                        ?: return@apply

                    if (currentItemIndex != -1) {
                        val currentItem = get(currentItemIndex)
                        set(currentItemIndex, updated.copy(
                            schedId = currentItem.schedId,
                            scheduledStartTimestamp = currentItem.scheduledStartTimestamp,
                            scheduledEndTimestamp = currentItem.scheduledEndTimestamp,
                        ))
                    } else {
                        add(updated)
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
                    set(currentItemIndex, updatedItem)
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

    private fun MutableList<MeetingRoomItem>.sortMeetings() =
        sortWith(compareByDescending<MeetingRoomItem> { it.isScheduledMeeting() }
            .thenBy { it.scheduledStartTimestamp }
            .thenByDescending { it.lastTimestamp }
        )
}
