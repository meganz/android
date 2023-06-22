package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.data.mapper.chat.ChatRoomItemMapper
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.IndividualChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItemStatus
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.ChatRoomItemStatusMapper
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import javax.inject.Inject

/**
 * Use case to retrieve Chat Rooms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val pushesRepository: PushesRepository,
    private val getScheduleMeetingDataUseCase: GetScheduleMeetingDataUseCase,
    private val getChatGroupAvatarUseCase: GetChatGroupAvatarUseCase,
    private val chatRoomItemMapper: ChatRoomItemMapper,
    private val chatRoomItemStatusMapper: ChatRoomItemStatusMapper,
    private val contactsRepository: ContactsRepository,
    private val getChatCall: GetChatCall,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val getUserEmail: GetContactEmail,
) {

    /**
     * Chat room request type
     */
    enum class ChatRoomType {
        /**
         * Meetings chat rooms
         */
        MEETINGS,

        /**
         * Non Meetings chat rooms
         */
        NON_MEETINGS,

        /**
         * Archived chat rooms
         */
        ARCHIVED_CHATS
    }

    lateinit var chatRoomType: ChatRoomType

    /**
     * Retrieve a flow of updated [ChatRoomItem]
     *
     * @param chatRoomType          [ChatRoomType]
     * @param lastMessage
     * @param lastTimeMapper
     * @param meetingTimeMapper
     * @param headerTimeMapper
     */
    operator fun invoke(
        chatRoomType: ChatRoomType = ChatRoomType.NON_MEETINGS,
        lastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
        headerTimeMapper: (ChatRoomItem, ChatRoomItem?) -> String?,
    ): Flow<List<ChatRoomItem>> =
        flow {
            this@GetChatsUseCase.chatRoomType = chatRoomType

            val mutex = Mutex()
            val chats = mutableMapOf<Long, ChatRoomItem>()

            emit(chats.addChatRooms())

            emitAll(
                merge(
                    chats.updateFields(mutex, lastMessage, lastTimeMapper, meetingTimeMapper),
                    chats.monitorMutedChats(mutex),
                    chats.monitorChatCalls(mutex),
                    chats.monitorChatOnlineStatusUpdates(mutex),
                    chats.monitorChatUpdates(mutex, lastMessage, lastTimeMapper, meetingTimeMapper),
                ).mapLatest {
                    it.sorted().addHeaders(headerTimeMapper)
                }
            )
        }

    private suspend fun MutableMap<Long, ChatRoomItem>.addChatRooms(): List<ChatRoomItem> =
        when (chatRoomType) {
            ChatRoomType.MEETINGS -> chatRepository.getMeetingChatRooms()
            ChatRoomType.NON_MEETINGS -> chatRepository.getNonMeetingChatRooms()
            ChatRoomType.ARCHIVED_CHATS -> chatRepository.getArchivedChatRooms()
        }
            .sortedByDescending(CombinedChatRoom::lastTimestamp)
            .forEach { chatRoom ->
                if (chatRoomType == ChatRoomType.ARCHIVED_CHATS || !chatRoom.isArchived) {
                    put(chatRoom.chatId, chatRoomItemMapper(chatRoom))
                }
            }.let { values.toList() }

    private fun MutableMap<Long, ChatRoomItem>.updateFields(
        mutex: Mutex,
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
    ): Flow<List<ChatRoomItem>> =
        flow {
            values.toList().forEach { currentItem ->
                val updatedItem = currentItem.updateChatFields(getLastMessage, lastTimeMapper)
                if (currentItem != updatedItem) {
                    mutex.withLock {
                        put(currentItem.chatId, updatedItem)
                    }
                    emit(values.toList())
                }

                if (currentItem is MeetingChatRoomItem) {
                    val meetingItem = updatedItem.updateMeetingFields(meetingTimeMapper)
                    if (updatedItem != meetingItem) {
                        mutex.withLock {
                            put(currentItem.chatId, meetingItem)
                        }
                        emit(values.toList())
                    }
                }
            }
        }

    private suspend fun ChatRoomItem.updateChatFields(
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
    ): ChatRoomItem = copyChatRoomItem(
        isMuted = isChatMuted(chatId),
        isLastMessageGeolocation = isLastMessageGeolocation(chatId),
        lastMessage = runCatching { getLastMessage(chatId) }.getOrNull(),
        lastTimestampFormatted = runCatching { lastTimeMapper(lastTimestamp) }.getOrNull(),
        currentCall = getCurrentCall(chatId),
        avatarItems = getParticipantsAvatar(chatId),
        userStatus = this.getUserOnlineStatus(),
        peerEmail = this.getUserEmail(),
    )

    private suspend fun ChatRoomItem.updateMeetingFields(
        meetingTimeMapper: (Long, Long) -> String,
    ): ChatRoomItem =
        if (this is MeetingChatRoomItem) {
            runCatching { getScheduleMeetingDataUseCase(chatId, meetingTimeMapper) }.getOrNull()
                ?.let { schedMeetingData ->
                    copyChatRoomItem(
                        schedId = schedMeetingData.schedId,
                        scheduledStartTimestamp = schedMeetingData.scheduledStartTimestamp,
                        scheduledEndTimestamp = schedMeetingData.scheduledEndTimestamp,
                        scheduledTimestampFormatted = schedMeetingData.scheduledTimestampFormatted,
                        isRecurringDaily = schedMeetingData.isRecurringDaily,
                        isRecurringWeekly = schedMeetingData.isRecurringWeekly,
                        isRecurringMonthly = schedMeetingData.isRecurringMonthly,
                        isPending = schedMeetingData.isPending,
                    )
                } ?: this
        } else this

    private fun MutableMap<Long, ChatRoomItem>.monitorMutedChats(mutex: Mutex): Flow<List<ChatRoomItem>> =
        pushesRepository.monitorPushNotificationSettings().mapNotNull {
            var listUpdated = false
            values.toList().forEach { item ->
                val itemMuted = isChatMuted(item.chatId)
                if (item.isMuted != itemMuted) {
                    listUpdated = true
                    mutex.withLock {
                        get(item.chatId)?.let { currentItem ->
                            val newItem = currentItem.copyChatRoomItem(
                                isMuted = itemMuted,
                            )

                            put(item.chatId, newItem)
                        }
                    }
                }
            }
            values.toList().takeIf { listUpdated }
        }

    private fun MutableMap<Long, ChatRoomItem>.monitorChatCalls(mutex: Mutex): Flow<List<ChatRoomItem>> =
        monitorChatCallUpdates()
            .filter { containsKey(it.chatId) }
            .mapNotNull { chatCall ->
                chatCall.let(chatRoomItemStatusMapper::invoke).let { chatCallItem ->
                    mutex.withLock {
                        get(chatCall.chatId)?.let { currentItem ->
                            val updatedItem = currentItem.copyChatRoomItem(
                                currentCall = chatCallItem
                            )

                            if (currentItem != updatedItem) {
                                put(chatCall.chatId, updatedItem)
                            }
                        }
                    }
                    values.toList()
                }
            }

    private fun MutableMap<Long, ChatRoomItem>.monitorChatUpdates(
        mutex: Mutex,
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
    ): Flow<List<ChatRoomItem>> =
        chatRepository.monitorChatListItemUpdates().mapNotNull { chatListItem ->
            if (((chatRoomType == ChatRoomType.ARCHIVED_CHATS && !chatListItem.isArchived) ||
                        chatListItem.isArchived) || chatListItem.isDeleted ||
                chatListItem.changes == ChatListItemChanges.Deleted ||
                chatListItem.changes == ChatListItemChanges.Closed
            ) {
                mutex.withLock { remove(chatListItem.chatId) }
                return@mapNotNull values.toList()
            }

            delay(500) // Required to wait for new SDK values

            chatRepository.getCombinedChatRoom(chatListItem.chatId)
                ?.let(chatRoomItemMapper::invoke)
                ?.updateChatFields(getLastMessage, lastTimeMapper)
                ?.updateMeetingFields(meetingTimeMapper)
                ?.let { newItem ->
                    mutex.withLock {
                        if (newItem != get(chatListItem.chatId)) {
                            put(chatListItem.chatId, newItem)
                        }
                    }
                    values.toList()
                }
        }

    private fun MutableMap<Long, ChatRoomItem>.monitorChatOnlineStatusUpdates(
        mutex: Mutex,
    ): Flow<List<ChatRoomItem>> =
        contactsRepository.monitorChatOnlineStatusUpdates().mapNotNull { update ->
            values.firstOrNull { item ->
                item is IndividualChatRoomItem && item.peerHandle == update.userHandle
            }?.chatId?.let { chatId ->
                mutex.withLock {
                    get(chatId)?.let { currentItem ->
                        val updatedItem = currentItem.copyChatRoomItem(
                            userStatus = update.status,
                        )

                        if (currentItem != updatedItem) {
                            put(updatedItem.chatId, updatedItem)
                        }
                    }
                }
                values.toList()
            }
        }

    private fun List<ChatRoomItem>.sorted(): List<ChatRoomItem> =
        if (chatRoomType == ChatRoomType.MEETINGS) {
            sortedWith { firstItem, secondItem ->
                when {
                    firstItem.isPendingMeeting() && secondItem.isPendingMeeting() -> {
                        firstItem as MeetingChatRoomItem
                        secondItem as MeetingChatRoomItem
                        when {
                            firstItem.scheduledStartTimestamp!! > secondItem.scheduledStartTimestamp!! -> 1
                            firstItem.scheduledStartTimestamp < secondItem.scheduledStartTimestamp -> -1
                            else -> 0
                        }
                    }

                    !firstItem.isPendingMeeting() && !secondItem.isPendingMeeting() -> {
                        when {
                            firstItem.highlight && !secondItem.highlight -> -1
                            !firstItem.highlight && secondItem.highlight -> 1
                            firstItem.lastTimestamp > secondItem.lastTimestamp -> -1
                            firstItem.lastTimestamp < secondItem.lastTimestamp -> 1
                            else -> 0
                        }
                    }

                    firstItem.isPendingMeeting() && !secondItem.isPendingMeeting() -> -1
                    else -> 1
                }
            }
        } else {
            sortedByDescending(ChatRoomItem::lastTimestamp)
        }

    private fun List<ChatRoomItem>.addHeaders(
        headerTimeMapper: (ChatRoomItem, ChatRoomItem?) -> String?,
    ): List<ChatRoomItem> =
        if (chatRoomType == ChatRoomType.MEETINGS) {
            mapIndexed { index, item ->
                val previousItem = getOrNull(index - 1)
                headerTimeMapper(item, previousItem)?.let { header ->
                    item.copyChatRoomItem(
                        header = header
                    )
                } ?: item
            }
        } else {
            this
        }

    private suspend fun getParticipantsAvatar(chatId: Long): List<ChatAvatarItem>? =
        runCatching { getChatGroupAvatarUseCase(chatId) }.getOrNull()

    private suspend fun ChatRoomItem.getUserOnlineStatus(): UserStatus? =
        runCatching {
            if (this is IndividualChatRoomItem && peerHandle != null)
                getUserOnlineStatusByHandleUseCase(peerHandle)
            else null
        }.getOrNull()

    private suspend fun ChatRoomItem.getUserEmail(): String? =
        runCatching {
            if (this is IndividualChatRoomItem && peerHandle != null)
                getUserEmail(peerHandle)
            else null
        }.getOrNull()

    private suspend fun isChatMuted(chatId: Long): Boolean =
        runCatching { !chatRepository.isChatNotifiable(chatId) }.getOrNull() ?: false

    private suspend fun isLastMessageGeolocation(chatId: Long): Boolean =
        runCatching { chatRepository.isChatLastMessageGeolocation(chatId) }.getOrNull() ?: false

    private suspend fun getCurrentCall(chatId: Long): ChatRoomItemStatus =
        runCatching { getChatCall(chatId)?.let(chatRoomItemStatusMapper::invoke) }.getOrNull()
            ?: ChatRoomItemStatus.NotStarted
}
