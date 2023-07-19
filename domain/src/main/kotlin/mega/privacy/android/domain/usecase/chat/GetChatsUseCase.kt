package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
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
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingData
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.ChatRoomItemStatusMapper
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingOccurrencesUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
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
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorScheduledMeetingOccurrencesUpdates: MonitorScheduledMeetingOccurrencesUpdates,
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
            val mutex = Mutex()
            val chats = mutableMapOf<Long, ChatRoomItem>()

            emit(chats.addChatRooms(chatRoomType))

            emitAll(
                merge(
                    merge(
                        *chats.updateFields(
                            mutex,
                            chatRoomType,
                            lastMessage,
                            lastTimeMapper,
                            meetingTimeMapper
                        ).toTypedArray()
                    ),
                    chats.monitorMutedChats(mutex, chatRoomType),
                    chats.monitorChatCalls(mutex, chatRoomType),
                    chats.monitorChatOnlineStatusUpdates(mutex, chatRoomType),
                    chats.monitorSchedMeetingUpdates(mutex, chatRoomType, meetingTimeMapper),
                    chats.monitorChatUpdates(
                        mutex,
                        chatRoomType,
                        lastMessage,
                        lastTimeMapper,
                        meetingTimeMapper
                    ),
                ).mapLatest {
                    it.sorted(chatRoomType).addHeaders(chatRoomType, headerTimeMapper)
                }
            )
        }

    private suspend fun MutableMap<Long, ChatRoomItem>.addChatRooms(
        chatRoomType: ChatRoomType,
    ): List<ChatRoomItem> =
        when (chatRoomType) {
            ChatRoomType.MEETINGS -> chatRepository.getMeetingChatRooms()
            ChatRoomType.NON_MEETINGS -> chatRepository.getNonMeetingChatRooms()
            ChatRoomType.ARCHIVED_CHATS -> chatRepository.getArchivedChatRooms()
        }
            .sortedByDescending(CombinedChatRoom::lastTimestamp)
            .forEach { chatRoom ->
                if (chatRoomType == ChatRoomType.ARCHIVED_CHATS
                    || (!chatRoom.isArchived && chatRoom.isActive)
                ) {
                    put(chatRoom.chatId, chatRoomItemMapper(chatRoom))
                }
            }.let { values.toList() }

    private fun MutableMap<Long, ChatRoomItem>.updateFields(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
    ): List<Flow<List<ChatRoomItem>>> =
        values.toList().map { currentItem ->
            flow {
                val newItem = currentItem.updateChatFields(getLastMessage, lastTimeMapper)
                if (currentItem != newItem) {
                    mutex.withLock {
                        put(currentItem.chatId, newItem)
                    }
                    emit(values.toList())
                }

                if (currentItem is MeetingChatRoomItem) {
                    val meetingItem = newItem.updateMeetingFields(chatRoomType, meetingTimeMapper)
                    if (newItem != meetingItem) {
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
    ): ChatRoomItem = coroutineScope {
        val avatarItems = async { getParticipantsAvatar(chatId) }
        val isMuted = async { isChatMuted(chatId) }
        val lastMessage = async { runCatching { getLastMessage(chatId) }.getOrNull() }
        val lastTimestampFormatted = async { runCatching { lastTimeMapper(lastTimestamp) }.getOrNull() }
        val currentCall = async { getCurrentCall(chatId) }
        val userStatus = async { getUserOnlineStatus() }
        val peerEmail = async { getUserEmail() }

        copyChatRoomItem(
            avatarItems = avatarItems.await(),
            isMuted = isMuted.await(),
            lastMessage = lastMessage.await(),
            lastTimestampFormatted = lastTimestampFormatted.await(),
            currentCall = currentCall.await(),
            userStatus = userStatus.await(),
            peerEmail = peerEmail.await(),
        )
    }

    private suspend fun ChatRoomItem.updateMeetingFields(
        chatRoomType: ChatRoomType,
        meetingTimeMapper: (Long, Long) -> String,
    ): ChatRoomItem =
        if (chatRoomType == ChatRoomType.MEETINGS && this is MeetingChatRoomItem) {
            getMeetingScheduleData(chatId, meetingTimeMapper)?.let { schedMeetingData ->
                copyChatRoomItem(
                    schedId = schedMeetingData.schedId,
                    isPending = schedMeetingData.isPending,
                    isRecurringDaily = schedMeetingData.isRecurringDaily,
                    isRecurringWeekly = schedMeetingData.isRecurringWeekly,
                    isRecurringMonthly = schedMeetingData.isRecurringMonthly,
                    scheduledStartTimestamp = schedMeetingData.scheduledStartTimestamp,
                    scheduledEndTimestamp = schedMeetingData.scheduledEndTimestamp,
                    scheduledTimestampFormatted = schedMeetingData.scheduledTimestampFormatted,
                )
            } ?: this
        } else this

    private fun MutableMap<Long, ChatRoomItem>.monitorMutedChats(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
    ): Flow<List<ChatRoomItem>> =
        if (chatRoomType != ChatRoomType.ARCHIVED_CHATS) {
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
        } else emptyFlow()

    private fun MutableMap<Long, ChatRoomItem>.monitorChatCalls(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
    ): Flow<List<ChatRoomItem>> =
        if (chatRoomType != ChatRoomType.ARCHIVED_CHATS) {
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
        } else emptyFlow()

    private fun MutableMap<Long, ChatRoomItem>.monitorSchedMeetingUpdates(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
        meetingTimeMapper: (Long, Long) -> String,
    ): Flow<List<ChatRoomItem>> =
        if (chatRoomType == ChatRoomType.MEETINGS) {
            merge(monitorScheduledMeetingUpdates(), monitorScheduledMeetingOccurrencesUpdates())
                .mapNotNull { update ->
                    when (update) {
                        is ResultOccurrenceUpdate -> update.chatId
                        is ChatScheduledMeeting -> update.chatId
                        else -> null
                    }
                }
                .filter(::containsKey)
                .mapNotNull { chatId ->
                    getMeetingScheduleData(chatId, meetingTimeMapper)?.let { schedData ->
                        mutex.withLock {
                            get(chatId)?.let { currentItem ->
                                val newItem = currentItem.copyChatRoomItem(
                                    schedId = schedData.schedId,
                                    isPending = schedData.isPending,
                                    isRecurringDaily = schedData.isRecurringDaily,
                                    isRecurringWeekly = schedData.isRecurringWeekly,
                                    isRecurringMonthly = schedData.isRecurringMonthly,
                                    scheduledStartTimestamp = schedData.scheduledStartTimestamp,
                                    scheduledEndTimestamp = schedData.scheduledEndTimestamp,
                                )
                                if (currentItem != newItem) {
                                    put(currentItem.chatId, newItem)
                                }
                            }
                        }
                        values.toList()
                    }
                }
        } else emptyFlow()

    private fun MutableMap<Long, ChatRoomItem>.monitorChatUpdates(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
    ): Flow<List<ChatRoomItem>> =
        chatRepository.monitorChatListItemUpdates().mapNotNull { chatListItem ->
            if (((chatRoomType == ChatRoomType.ARCHIVED_CHATS && !chatListItem.isArchived) ||
                        chatListItem.isArchived) || chatListItem.isDeleted || !chatListItem.isActive
                || chatListItem.changes == ChatListItemChanges.Deleted ||
                chatListItem.changes == ChatListItemChanges.Closed
            ) {
                mutex.withLock { remove(chatListItem.chatId) }
                return@mapNotNull values.toList()
            }

            delay(500) // Required to wait for new SDK values

            chatRepository.getCombinedChatRoom(chatListItem.chatId)
                ?.takeIf {
                    (it.isMeeting && chatRoomType == ChatRoomType.MEETINGS)
                            || (!it.isMeeting && chatRoomType == ChatRoomType.NON_MEETINGS)
                }
                ?.let(chatRoomItemMapper::invoke)
                ?.updateChatFields(getLastMessage, lastTimeMapper)
                ?.updateMeetingFields(chatRoomType, meetingTimeMapper)
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
        chatRoomType: ChatRoomType,
    ): Flow<List<ChatRoomItem>> =
        if (chatRoomType != ChatRoomType.ARCHIVED_CHATS) {
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
        } else emptyFlow()

    private fun List<ChatRoomItem>.sorted(chatRoomType: ChatRoomType): List<ChatRoomItem> =
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
        chatRoomType: ChatRoomType,
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

    private suspend fun getMeetingScheduleData(
        chatId: Long,
        meetingTimeMapper: (Long, Long) -> String,
    ): ScheduledMeetingData? =
        runCatching { getScheduleMeetingDataUseCase(chatId, meetingTimeMapper) }.getOrNull()
}
