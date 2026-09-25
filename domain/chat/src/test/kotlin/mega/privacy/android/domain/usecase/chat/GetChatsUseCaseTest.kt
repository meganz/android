package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.chat.ChatRoomItemMapper
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.OnlineStatus
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetChatsUseCaseTest {

    private lateinit var underTest: GetChatsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private val chatRepository = mock<ChatRepository>()
    private val pushesRepository = mock<PushesRepository>()
    private val getScheduleMeetingDataUseCase = mock<GetScheduleMeetingDataUseCase>()
    private val getChatGroupAvatarUseCase = mock<GetChatGroupAvatarUseCase>()
    private val chatRoomItemMapper = mock<ChatRoomItemMapper>()
    private val chatRoomItemStatusMapper = mock<ChatRoomItemStatusMapper>()
    private val contactsRepository = mock<ContactsRepository>()
    private val getChatCallUseCase = mock<GetChatCallUseCase>()
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val getUserOnlineStatusByHandleUseCase = mock<GetUserOnlineStatusByHandleUseCase>()
    private val getUserEmail = mock<GetContactEmail>()
    private val monitorScheduledMeetingUpdatesUseCase =
        mock<MonitorScheduledMeetingUpdatesUseCase>()
    private val monitorScheduledMeetingOccurrencesUpdatesUseCase =
        mock<MonitorScheduledMeetingOccurrencesUpdatesUseCase>()
    private val notificationsRepository = mock<NotificationsRepository>()
    private val getArchivedChatRoomsUseCase = mock<GetArchivedChatRoomsUseCase>()
    private val chatParticipantsRepository = mock<ChatParticipantsRepository>()

    private val lastMessage: suspend (Long) -> String = { "test" }
    private val lastTimeMapper: (Long) -> String = { "test" }
    private val meetingTimeMapper: (Long, Long) -> String = { _, _ -> "test" }
    private val headerTimeMapper: (ChatRoomItem, ChatRoomItem?) -> String = { _, _ -> "test" }

    private val chatRooms = listOf(
        CombinedChatRoom(
            chatId = 1L,
            lastTimestamp = -1L,
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 2L,
            lastTimestamp = -2L,
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 3L,
            lastTimestamp = -3L,
            isActive = true
        ),
        CombinedChatRoom(
            chatId = 4L,
            lastTimestamp = -4L,
            isActive = true
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        underTest = GetChatsUseCase(
            chatRepository,
            pushesRepository,
            chatParticipantsRepository,
            getScheduleMeetingDataUseCase,
            getChatGroupAvatarUseCase,
            chatRoomItemMapper,
            chatRoomItemStatusMapper,
            contactsRepository,
            getChatCallUseCase,
            monitorChatCallUpdatesUseCase,
            getUserOnlineStatusByHandleUseCase,
            getUserEmail,
            monitorScheduledMeetingUpdatesUseCase,
            monitorScheduledMeetingOccurrencesUpdatesUseCase,
            notificationsRepository,
            getArchivedChatRoomsUseCase
        )

        runBlocking {
            whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(chatRooms)
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(chatRooms)
            whenever(getArchivedChatRoomsUseCase()).thenReturn(chatRooms)
            whenever(chatRepository.isChatNotifiable(any())).thenReturn(Random.nextBoolean())
            whenever(getChatCallUseCase(any())).thenReturn(null)
            whenever(getChatGroupAvatarUseCase(any())).thenReturn(null)
            whenever(getUserOnlineStatusByHandleUseCase(any())).thenReturn(null)
            whenever(getUserEmail(any())).thenReturn(null)
            whenever(getScheduleMeetingDataUseCase.invoke(any(), any())).thenReturn(null)
            whenever(pushesRepository.monitorPushNotificationSettings()).thenReturn(emptyFlow())
            whenever(monitorChatCallUpdatesUseCase.invoke()).thenReturn(emptyFlow())
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
            whenever(contactsRepository.monitorChatOnlineStatusUpdates()).thenReturn(emptyFlow())
            whenever(monitorScheduledMeetingUpdatesUseCase()).thenReturn(emptyFlow())
            whenever(monitorScheduledMeetingOccurrencesUpdatesUseCase()).thenReturn(emptyFlow())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that meetings are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS
            val chatRoomItem = ChatRoomItem.GroupChatRoomItem(
                chatId = chatRooms.first().chatId,
                title = chatRooms.first().title
            )
            whenever(chatRoomItemMapper.invoke(any())).thenReturn(chatRoomItem)
            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(chatRepository).getMeetingChatRooms()
        }

    @Test
    fun `test that non meetings are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
            val chatRoomItem = ChatRoomItem.GroupChatRoomItem(
                chatId = chatRooms.first().chatId,
                title = chatRooms.first().title
            )
            whenever(chatRoomItemMapper.invoke(any())).thenReturn(chatRoomItem)
            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(chatRepository).getNonMeetingChatRooms()
        }

    @Test
    fun `test that archived are retrieved based chat room type parameter`() =
        runTest {
            val chatRoomType = GetChatsUseCase.ChatRoomType.ARCHIVED_CHATS
            val chatRoomItem = ChatRoomItem.GroupChatRoomItem(
                chatId = chatRooms.first().chatId,
                title = chatRooms.first().title
            )
            whenever(chatRoomItemMapper.invoke(any())).thenReturn(chatRoomItem)

            underTest.invoke(
                chatRoomType = chatRoomType,
                lastMessage = lastMessage,
                lastTimeMapper = lastTimeMapper,
                meetingTimeMapper = meetingTimeMapper,
                headerTimeMapper = headerTimeMapper,
            ).first()

            verify(getArchivedChatRoomsUseCase).invoke()
        }

    @Test
    fun `test that chat rooms are sorted by last timestamp`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        val sortedChatRooms = chatRooms.sortedByDescending(CombinedChatRoom::lastTimestamp)

        assertThat(result.first().chatId).isEqualTo(sortedChatRooms.first().chatId)
    }

    @Test
    fun `test that isChatDndEnabled is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(notificationsRepository, times(chatRooms.size)).isChatDndEnabled(anyLong())
    }

    @Test
    fun `test that getCall is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(
                chatId = chatRoom.chatId,
                call = mock<ChatCall>(),
                currentCallStatus = ChatRoomItemStatus.Joined,
                title = chatRoom.title
            )
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getChatCallUseCase, times(chatRooms.size)).invoke(anyLong())
    }

    @Test
    fun `test that getChatGroupAvatarUseCase is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getChatGroupAvatarUseCase, times(chatRooms.size)).invoke(anyLong())
    }

    @Test
    fun `test that getScheduleMeetingDataUseCase is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.MeetingChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getScheduleMeetingDataUseCase, times(chatRooms.size)).invoke(anyLong(), any())
    }

    @Test
    fun `test that getUserOnlineStatusByHandleUseCase is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.IndividualChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title,
                peerHandle = Random.nextLong()
            )
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getUserOnlineStatusByHandleUseCase, times(chatRooms.size)).invoke(anyLong())
    }

    @Test
    fun `test that getUserEmail is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.IndividualChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title,
                peerHandle = Random.nextLong()
            )
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getUserEmail, times(chatRooms.size)).invoke(anyLong())
    }

    @Test
    fun `test that monitorChatCallUpdates is called accordingly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(monitorChatCallUpdatesUseCase, times(1)).invoke()
    }

    @Test
    fun `test that note to self chats are sorted first`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            if (chatRoom.chatId == 1L) {
                ChatRoomItem.NoteToSelfChatRoomItem(
                    chatId = chatRoom.chatId,
                    title = chatRoom.title
                )
            } else {
                ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
            }
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        assertThat(result.first()).isInstanceOf(ChatRoomItem.NoteToSelfChatRoomItem::class.java)
        assertThat(result.first().chatId).isEqualTo(1L)
    }

    @Test
    fun `test that meeting chats are sorted correctly with pending meetings first`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.MeetingChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title,
                scheduledStartTimestamp = when (chatRoom.chatId) {
                    1L -> 1000L
                    2L -> 2000L
                    3L -> 3000L
                    4L -> 4000L
                    else -> 0L
                },
                isPending = chatRoom.chatId <= 2L
            )
        }

        whenever(getScheduleMeetingDataUseCase.invoke(any(), any())).thenAnswer {
            val chatId = it.arguments[0] as Long
            ScheduledMeetingData(
                schedId = chatId,
                title = "Meeting $chatId",
                isPending = chatId <= 2L,
                scheduledStartTimestamp = when (chatId) {
                    1L -> 1000L
                    2L -> 2000L
                    3L -> 3000L
                    4L -> 4000L
                    else -> 0L
                },
                scheduledEndTimestamp = 0L,
                scheduledTimestampFormatted = "test",
                isRecurringDaily = false,
                isRecurringWeekly = false,
                isRecurringMonthly = false,
                isCancelled = false
            )
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        // Pending meetings should come first, sorted by scheduled start time (earliest first)
        assertThat(result[0].chatId).isEqualTo(1L) // Earliest pending meeting
        assertThat(result[1].chatId).isEqualTo(2L) // Latest pending meeting
    }

    @Test
    fun `test that headers are added for meeting chats`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.MeetingChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title
            )
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        // Headers should be added for meeting chats
        assertThat(result.all { it.header == "test" }).isTrue()
    }

    @Test
    fun `test that headers are not added for non-meeting chats`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        // Headers should not be added for non-meeting chats
        assertThat(result.any { it.header != null }).isFalse()
    }

    @Test
    fun `test that muted chat monitoring works correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        whenever(pushesRepository.monitorPushNotificationSettings()).thenReturn(
            flowOf(true)
        )

        whenever(notificationsRepository.isChatDndEnabled(any())).thenReturn(true)

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(pushesRepository).monitorPushNotificationSettings()
    }

    @Test
    fun `test that chat call monitoring works correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
        val mockCall = mock<ChatCall>()

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        whenever(monitorChatCallUpdatesUseCase.invoke()).thenReturn(
            flowOf(mockCall)
        )

        whenever(mockCall.chatId).thenReturn(1L)
        whenever(chatRoomItemStatusMapper.invoke(mockCall)).thenReturn(ChatRoomItemStatus.Joined)

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(monitorChatCallUpdatesUseCase).invoke()
    }

    @Test
    fun `test that scheduled meeting updates monitoring works correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.MeetingChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        whenever(monitorScheduledMeetingUpdatesUseCase()).thenReturn(
            flowOf(mock<ChatScheduledMeeting>())
        )

        whenever(monitorScheduledMeetingOccurrencesUpdatesUseCase()).thenReturn(
            flowOf(mock<ResultOccurrenceUpdate>())
        )

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(monitorScheduledMeetingUpdatesUseCase).invoke()
        verify(monitorScheduledMeetingOccurrencesUpdatesUseCase).invoke()
    }

    @Test
    fun `test that online status updates monitoring works correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.IndividualChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title,
                peerHandle = chatRoom.chatId
            )
        }

        whenever(contactsRepository.monitorChatOnlineStatusUpdates()).thenReturn(
            flowOf(mock<OnlineStatus>())
        )

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(contactsRepository).monitorChatOnlineStatusUpdates()
    }

    @Test
    fun `test that chat updates monitoring works correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
        val mockChatListItem = mock<ChatListItem>()

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(
            flowOf(mockChatListItem)
        )

        whenever(mockChatListItem.chatId).thenReturn(1L)
        whenever(mockChatListItem.isArchived).thenReturn(false)
        whenever(mockChatListItem.isDeleted).thenReturn(false)
        whenever(mockChatListItem.isPreview).thenReturn(false)
        whenever(mockChatListItem.changes).thenReturn(ChatListItemChanges.Title)

        whenever(chatRepository.getCombinedChatRoom(any())).thenReturn(
            CombinedChatRoom(
                chatId = 1L,
                lastTimestamp = 1000L,
                isActive = true
            )
        )

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(chatRepository).monitorChatListItemUpdates()
    }

    @Test
    fun `test that archived chats are filtered correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.ARCHIVED_CHATS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        verify(getArchivedChatRoomsUseCase).invoke()
    }

    @Test
    fun `test that preview chats are filtered out`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
        val previewChatRooms = listOf(
            CombinedChatRoom(
                chatId = 1L,
                lastTimestamp = -1L,
                isActive = true,
                isPreview = true
            )
        )

        whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(previewChatRooms)

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that invalid chat IDs are filtered out`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS
        val invalidChatRooms = listOf(
            CombinedChatRoom(
                chatId = -1L,
                lastTimestamp = -1L,
                isActive = true
            )
        )

        whenever(chatRepository.getNonMeetingChatRooms()).thenReturn(invalidChatRooms)

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that chat participants repository is called for user attributes`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(
                chatId = chatRoom.chatId,
                title = chatRoom.title,
                peers = listOf(chatRoom.chatId)
            )
        }

        whenever(chatParticipantsRepository.getUserEmailFromCache(any())).thenReturn(null)
        whenever(chatParticipantsRepository.getUserFullNameFromCache(any())).thenReturn(null)

        underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(chatParticipantsRepository, times(chatRooms.size * 2)).loadUserAttributes(any(), any())
    }

    @Test
    fun `test that error handling works for failed operations`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        // Simulate failures in various operations
        whenever(getChatGroupAvatarUseCase(any())).thenThrow(RuntimeException("Avatar error"))
        whenever(notificationsRepository.isChatDndEnabled(any())).thenThrow(RuntimeException("DND error"))
        whenever(getChatCallUseCase(any())).thenThrow(RuntimeException("Call error"))

        // Should not crash and should return empty results for failed operations
        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        // Verify that the use case handles errors gracefully
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `test that meeting fields are updated correctly`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.MeetingChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        whenever(getScheduleMeetingDataUseCase.invoke(any(), any())).thenAnswer {
            val chatId = it.arguments[0] as Long
            ScheduledMeetingData(
                schedId = chatId,
                title = "Updated Meeting $chatId",
                isPending = true,
                scheduledStartTimestamp = 1000L,
                scheduledEndTimestamp = 2000L,
                scheduledTimestampFormatted = "test",
                isRecurringDaily = true,
                isRecurringWeekly = false,
                isRecurringMonthly = false,
                isCancelled = false
            )
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        verify(getScheduleMeetingDataUseCase, times(chatRooms.size)).invoke(any(), any())
    }

    @Test
    fun `test that non-meeting chat rooms are not updated with meeting fields`() = runTest {
        val chatRoomType = GetChatsUseCase.ChatRoomType.NON_MEETINGS

        whenever(chatRoomItemMapper(any())).thenAnswer {
            val chatRoom = ((it.arguments[0]) as CombinedChatRoom)
            ChatRoomItem.GroupChatRoomItem(chatId = chatRoom.chatId, title = chatRoom.title)
        }

        val result = underTest.invoke(
            chatRoomType = chatRoomType,
            lastMessage = lastMessage,
            lastTimeMapper = lastTimeMapper,
            meetingTimeMapper = meetingTimeMapper,
            headerTimeMapper = headerTimeMapper,
        ).take(2).last()

        // Should not call meeting-related operations for non-meeting chats
        verify(getScheduleMeetingDataUseCase, times(0)).invoke(any(), any())
    }
}
