package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.data.mapper.chat.ChatRoomItemMapper
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.IndividualChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingData
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.ChatRoomItemStatusMapper
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingOccurrencesUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import javax.inject.Inject

/**
 * Use case to retrieve Chat Rooms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val pushesRepository: PushesRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val getScheduleMeetingDataUseCase: GetScheduleMeetingDataUseCase,
    private val getChatGroupAvatarUseCase: GetChatGroupAvatarUseCase,
    private val chatRoomItemMapper: ChatRoomItemMapper,
    private val chatRoomItemStatusMapper: ChatRoomItemStatusMapper,
    private val contactsRepository: ContactsRepository,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val getUserEmail: GetContactEmail,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val monitorScheduledMeetingOccurrencesUpdatesUseCase: MonitorScheduledMeetingOccurrencesUpdatesUseCase,
    private val notificationsRepository: NotificationsRepository,
    private val getArchivedChatRoomsUseCase: GetArchivedChatRoomsUseCase,
) {

    companion object {
        private const val MAX_CONCURRENT_JOBS = 8
    }

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
            emit(sortChats(chats.addChatRooms(mutex, chatRoomType)))

            emitAll(
                flowOf(
                    chats.updateFields(
                        mutex,
                        chatRoomType,
                        lastMessage,
                        lastTimeMapper,
                        meetingTimeMapper
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
                ).flattenMerge().mapLatest {
                    sortChats(it.sorted(chatRoomType).addHeaders(chatRoomType, headerTimeMapper))
                }
            )
        }


    /**
     * Sort chats by note to self chat
     *
     * @param items List of [ChatRoomItem]
     */
    private fun sortChats(items: List<ChatRoomItem>): List<ChatRoomItem> =
        items.sortedByDescending { it is ChatRoomItem.NoteToSelfChatRoomItem }

    private suspend fun MutableMap<Long, ChatRoomItem>.addChatRooms(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
    ): List<ChatRoomItem> =
        when (chatRoomType) {
            ChatRoomType.MEETINGS -> chatRepository.getMeetingChatRooms()
            ChatRoomType.NON_MEETINGS -> chatRepository.getNonMeetingChatRooms()
            ChatRoomType.ARCHIVED_CHATS -> getArchivedChatRoomsUseCase()
        }
            .sortedByDescending(CombinedChatRoom::lastTimestamp)
            .forEach { chatRoom ->
                if (!chatRoom.isPreview && chatRoom.chatId != -1L && (chatRoomType == ChatRoomType.ARCHIVED_CHATS || !chatRoom.isArchived)) {
                    mutex.withLock { put(chatRoom.chatId, chatRoomItemMapper(chatRoom)) }
                }
            }.let { values.toList() }

    private fun MutableMap<Long, ChatRoomItem>.updateFields(
        mutex: Mutex,
        chatRoomType: ChatRoomType,
        getLastMessage: suspend (Long) -> String,
        lastTimeMapper: (Long) -> String,
        meetingTimeMapper: (Long, Long) -> String,
    ): Flow<List<ChatRoomItem>> {
        val copy = values.toMutableList()
        return copy.asFlow().flatMapMerge(MAX_CONCURRENT_JOBS) { currentItem ->
            flow {
                val newItem = currentItem.updateChatFields(getLastMessage, lastTimeMapper)
                val updatedItem = if (currentItem is MeetingChatRoomItem) {
                    newItem.updateMeetingFields(chatRoomType, meetingTimeMapper)
                } else {
                    newItem
                }

                mutex.withLock {
                    put(currentItem.chatId, updatedItem)
                }

                emit(values.toList())
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
        val lastTimestampFormatted =
            async { runCatching { lastTimeMapper(lastTimestamp) }.getOrNull() }
        val call = async { getCall(chatId) }
        val userStatus = async { getUserOnlineStatus() }
        val peerEmail = async { getUserEmail() }
        val description = async {
            val emails = peers.map { handle ->
                chatParticipantsRepository.getUserEmailFromCache(handle)
                    .takeIf { it?.isNotEmpty() == true } ?: run {
                    chatParticipantsRepository.loadUserAttributes(
                        chatId = chatId,
                        usersHandles = peers
                    ) // load all the attributes of the user so that in the next iteration other values are fetched form cache
                    chatParticipantsRepository.getUserEmailFromCache(handle)
                }
            }.joinToString(" ")

            val names = peers.map { handle ->
                chatParticipantsRepository.getUserFullNameFromCache(handle)
                    .takeIf { it?.isNotEmpty() == true } ?: run {
                    chatParticipantsRepository.loadUserAttributes(
                        chatId = chatId,
                        usersHandles = peers
                    ) // load all the attributes of the user so that in the next iteration other values are fetched form cache
                    chatParticipantsRepository.getUserFullNameFromCache(handle)
                }
            }.joinToString(" ")

            "$title $emails $names".trim()
        }

        copyChatRoomItem(
            avatarItems = avatarItems.await(),
            isMuted = isMuted.await(),
            lastMessage = lastMessage.await(),
            lastTimestampFormatted = lastTimestampFormatted.await(),
            currentCallStatus = call.await()?.let {
                chatRoomItemStatusMapper(it)
            } ?: ChatRoomItemStatus.NotStarted,
            call = call.await(),
            userChatStatus = userStatus.await(),
            peerEmail = peerEmail.await(),
            description = description.await()
        )
    }

    private suspend fun ChatRoomItem.updateMeetingFields(
        chatRoomType: ChatRoomType,
        meetingTimeMapper: (Long, Long) -> String,
    ): ChatRoomItem =
        if (chatRoomType == ChatRoomType.MEETINGS && this is MeetingChatRoomItem) {
            getMeetingScheduleData(chatId, meetingTimeMapper)?.let { schedMeetingData ->
                copyChatRoomItem(
                    title = schedMeetingData.title ?: title,
                    schedId = schedMeetingData.schedId,
                    isPending = schedMeetingData.isPending,
                    isRecurringDaily = schedMeetingData.isRecurringDaily,
                    isRecurringWeekly = schedMeetingData.isRecurringWeekly,
                    isRecurringMonthly = schedMeetingData.isRecurringMonthly,
                    scheduledStartTimestamp = schedMeetingData.scheduledStartTimestamp,
                    scheduledEndTimestamp = schedMeetingData.scheduledEndTimestamp,
                    scheduledTimestampFormatted = schedMeetingData.scheduledTimestampFormatted,
                    isCancelled = schedMeetingData.isCancelled
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
                mutex.withLock {
                    values.toList().forEach { item ->
                        val itemMuted = isChatMuted(item.chatId)
                        if (item.isMuted != itemMuted) {
                            listUpdated = true

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
            monitorChatCallUpdatesUseCase()
                .filter { containsKey(it.chatId) }
                .mapNotNull { chatCall ->
                    chatCall.let(chatRoomItemStatusMapper::invoke).let { chatCallItem ->
                        mutex.withLock {
                            get(chatCall.chatId)?.let { currentItem ->
                                val updatedItem = currentItem.copyChatRoomItem(
                                    call = chatCall,
                                    currentCallStatus = chatCallItem
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
    ): Flow<List<ChatRoomItem>> = if (chatRoomType == ChatRoomType.MEETINGS) {
        merge(
            monitorScheduledMeetingUpdatesUseCase(),
            monitorScheduledMeetingOccurrencesUpdatesUseCase()
        ).mapNotNull { update ->
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
                                title = schedData.title ?: currentItem.title,
                                isPending = schedData.isPending,
                                isRecurringDaily = schedData.isRecurringDaily,
                                isRecurringWeekly = schedData.isRecurringWeekly,
                                isRecurringMonthly = schedData.isRecurringMonthly,
                                scheduledStartTimestamp = schedData.scheduledStartTimestamp,
                                scheduledEndTimestamp = schedData.scheduledEndTimestamp,
                                isCancelled = schedData.isCancelled,
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
            if (
                (chatRoomType == ChatRoomType.ARCHIVED_CHATS && !chatListItem.isArchived) ||
                (chatRoomType != ChatRoomType.ARCHIVED_CHATS && chatListItem.isArchived) ||
                chatListItem.isDeleted || chatListItem.isPreview ||
                chatListItem.changes == ChatListItemChanges.Deleted ||
                chatListItem.changes == ChatListItemChanges.Closed
            ) {
                mutex.withLock { remove(chatListItem.chatId) }
                return@mapNotNull values.toList()
            }

            delay(500) // Required to wait for new SDK values

            chatRepository.getCombinedChatRoom(chatListItem.chatId)
                ?.takeIf {
                    when (chatRoomType) {
                        ChatRoomType.MEETINGS -> it.isMeeting && !it.isArchived && !it.isPreview
                        ChatRoomType.NON_MEETINGS -> !it.isMeeting && !it.isArchived
                        ChatRoomType.ARCHIVED_CHATS -> it.isArchived
                    }
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
                                userChatStatus = update.status,
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
                    (firstItem.isPendingMeeting() && secondItem.isPendingMeeting()) -> {
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

    private suspend fun ChatRoomItem.getUserOnlineStatus(): UserChatStatus? =
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
        runCatching { notificationsRepository.isChatDndEnabled(chatId) }.getOrNull() ?: false

    private suspend fun getCall(chatId: Long): ChatCall? =
        runCatching { getChatCallUseCase(chatId) }.getOrNull()

    private suspend fun getMeetingScheduleData(
        chatId: Long,
        meetingTimeMapper: (Long, Long) -> String,
    ): ScheduledMeetingData? =
        runCatching { getScheduleMeetingDataUseCase(chatId, meetingTimeMapper) }.getOrNull()
}
