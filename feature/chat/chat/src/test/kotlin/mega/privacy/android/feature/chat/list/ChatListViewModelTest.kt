package mega.privacy.android.feature.chat.list

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomActionPluralMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTitleMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.menu.ArchiveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_ARCHIVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_LEAVE_TAG
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_MUTE_TAG
import mega.privacy.android.feature.chat.list.menu.ChatRoomActions
import mega.privacy.android.feature.chat.list.menu.ChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.ClearHistoryChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.LeaveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.MuteChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.UnarchiveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.MeetingAction
import mega.privacy.android.feature.chat.list.model.MeetingActionEvent
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.NewChatNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ChatListViewModelTest {

    private lateinit var underTest: ChatListViewModel

    private lateinit var applicationScope: CoroutineScope

    private val getChatsUseCase = mock<GetChatsUseCase>()
    private val chatLastMessageFormatter = mock<ChatLastMessageFormatter>()
    private val chatRoomTimestampMapper = mock<ChatRoomTimestampMapper>()
    private val chatRoomUiItemMapper = mock<ChatRoomUiItemMapper>()
    private val muteChatNotificationForChatRoomsUseCase =
        mock<MuteChatNotificationForChatRoomsUseCase>()
    private val unmuteChatNotificationUseCase = mock<UnmuteChatNotificationUseCase>()
    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val leaveChatUseCase = mock<LeaveChatUseCase>()
    private val getContactHandleUseCase = mock<GetContactHandleUseCase>()
    private val startConversationUseCase = mock<StartConversationUseCase>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()
    private val monitorHasAnyContactUseCase = mock<MonitorHasAnyContactUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val chatRoomTitleMapper = mock<ChatRoomTitleMapper>()

    private val chatRoomMenuItems: Map<Int, ChatRoomMenuItem> = mapOf(
        0 to ClearHistoryChatRoomMenuItem(clearChatHistoryUseCase),
        1 to MuteChatRoomMenuItem(
            muteChatNotificationForChatRoomsUseCase,
            unmuteChatNotificationUseCase,
        ),
        2 to ArchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
        3 to UnarchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
        4 to LeaveChatRoomMenuItem(leaveChatUseCase),
    )

    private val chatRoomActionPluralMapper = mock<ChatRoomActionPluralMapper>()

    private val chatRoomActions = ChatRoomActions(
        chatRoomMenuItems = chatRoomMenuItems,
        chatRoomActionPluralMapper = chatRoomActionPluralMapper,
        snackbarEventQueue = snackbarEventQueue,
    )

    private val chatRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Chat",
    )
    private val meetingRoomItem = ChatRoomItem.MeetingChatRoomItem(
        chatId = 2L,
        title = "Meeting",
    )
    private val chatUiItem = chatRoomUiItem(chatId = 1L, title = "Chat")
    private val meetingUiItem = chatRoomUiItem(chatId = 2L, title = "Meeting")

    private var capturedLastMessage: (suspend (Long) -> String)? = null
    private var capturedLastTimeMapper: ((Long) -> String)? = null
    private var capturedMeetingTimeMapper: ((Long, Long) -> String)? = null
    private var capturedHeaderTimeMapper: ((ChatRoomItem, ChatRoomItem?) -> String?)? = null

    @BeforeEach
    fun setUp() {
        stubHasAnyContact(hasAnyContact = false)
        applicationScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        underTest = ChatListViewModel(
            getChatsUseCase = getChatsUseCase,
            chatLastMessageFormatter = chatLastMessageFormatter,
            chatRoomTimestampMapper = chatRoomTimestampMapper,
            chatRoomUiItemMapper = chatRoomUiItemMapper,
            getContactHandleUseCase = getContactHandleUseCase,
            startConversationUseCase = startConversationUseCase,
            createGroupChatRoomUseCase = createGroupChatRoomUseCase,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            monitorHasAnyContactUseCase = monitorHasAnyContactUseCase,
            chatRoomActions = chatRoomActions,
            applicationScope = applicationScope,
        )
    }

    @AfterEach
    fun tearDown() {
        applicationScope.cancel()
        capturedLastMessage = null
        capturedLastTimeMapper = null
        capturedMeetingTimeMapper = null
        capturedHeaderTimeMapper = null
        reset(
            getChatsUseCase,
            chatLastMessageFormatter,
            chatRoomTimestampMapper,
            chatRoomUiItemMapper,
            muteChatNotificationForChatRoomsUseCase,
            unmuteChatNotificationUseCase,
            archiveChatUseCase,
            clearChatHistoryUseCase,
            leaveChatUseCase,
            getContactHandleUseCase,
            isParticipatingInChatCallUseCase,
            startConversationUseCase,
            createGroupChatRoomUseCase,
            monitorHasAnyContactUseCase,
            snackbarEventQueue,
            chatRoomTitleMapper,
            chatRoomActionPluralMapper,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubChatRooms()

        assertThat(underTest.uiState.value).isEqualTo(ChatListUiState.Loading)
    }

    @Test
    fun `test that uiState emits Data with mapped chats and meetings when chat rooms are loaded`() =
        runTest {
            stubChatRooms()

            underTest.uiState.test {
                val actual = awaitDataState()

                assertThat(actual.chats.items()).containsExactly(chatUiItem)
                assertThat(actual.meetings.items()).containsExactly(meetingUiItem)
            }
        }

    @Test
    fun `test that a tab with no chat rooms is Empty when there is no search query`() = runTest {
        stubChatRooms(chats = emptyList(), meetings = emptyList())

        underTest.uiState.test {
            val actual = awaitDataState()

            assertThat(actual.chats).isEqualTo(ChatListTabState.Empty)
            assertThat(actual.meetings).isEqualTo(ChatListTabState.Empty)
        }
    }

    @Test
    fun `test that onSearchQueryChange filters chats by title`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("ali")

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(aliceUiItem)
        }
    }

    @Test
    fun `test that onSearchQueryChange filters chats by last message`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("dinner")

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(bobUiItem)
        }
    }

    @Test
    fun `test that a blank search query restores the full chat list`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()
            underTest.onSearchQueryChange("ali")
            awaitDataState()

            underTest.onSearchQueryChange(null)

            val actual = awaitDataState()
            assertThat(actual.chats.items()).containsExactly(aliceUiItem, bobUiItem)
        }
    }

    @Test
    fun `test that a search query with no matches emits NoSearchResults`() = runTest {
        stubSearchableChats()

        underTest.uiState.test {
            awaitDataState()

            underTest.onSearchQueryChange("zzz")

            val actual = awaitDataState()
            assertThat(actual.chats).isEqualTo(ChatListTabState.NoSearchResults)
        }
    }

    @Test
    fun `test that last message lambda delegates to the chat last message formatter`() =
        runTest {
            stubChatRooms()
            whenever(chatLastMessageFormatter(1L)) doReturn "Formatted preview"

            underTest.uiState.test { awaitDataState() }

            assertThat(capturedLastMessage?.invoke(1L)).isEqualTo("Formatted preview")
        }

    @Test
    fun `test that timestamp lambdas delegate to the timestamp mapper`() = runTest {
        stubChatRooms()
        whenever(chatRoomTimestampMapper.getLastTimeFormatted(100L)) doReturn "last time"
        whenever(chatRoomTimestampMapper.getMeetingTimeFormatted(100L, 200L)) doReturn "range"

        underTest.uiState.test { awaitDataState() }

        assertThat(capturedLastTimeMapper?.invoke(100L)).isEqualTo("last time")
        assertThat(capturedMeetingTimeMapper?.invoke(100L, 200L)).isEqualTo("range")
    }

    @Test
    fun `test that header time mapper lambda delegates to the timestamp mapper`() = runTest {
        stubChatRooms()
        whenever(chatRoomTimestampMapper.getHeaderTimeFormatted(meetingRoomItem, null))
            .thenReturn("Today")

        underTest.uiState.test { awaitDataState() }

        assertThat(capturedHeaderTimeMapper?.invoke(meetingRoomItem, null)).isEqualTo("Today")
    }

    @Test
    fun `test that a tab has no unread badge when no chat room is unread`() = runTest {
        stubChatRooms()

        underTest.uiState.test {
            val actual = awaitDataState()

            assertThat(actual.chatsHaveUnread).isFalse()
            assertThat(actual.meetingsHaveUnread).isFalse()
        }
    }

    @Test
    fun `test that a tab has an unread badge when a chat room has unread messages`() = runTest {
        stubChatRooms()
        chatRoomUiItemMapper.stub {
            on { invoke(chatRoomItem) } doReturn chatRoomUiItem(1L, "Chat", unreadCount = 3)
            on { invoke(meetingRoomItem) } doReturn meetingUiItem
        }

        underTest.uiState.test {
            val actual = awaitDataState()

            assertThat(actual.chatsHaveUnread).isTrue()
            assertThat(actual.meetingsHaveUnread).isFalse()
        }
    }

    @Test
    fun `test that getBulkChatRoomActions returns the eligible actions ordered for a single selected chat`() =
        runTest {
            stubChatRooms(
                chats = listOf(
                    ChatRoomItem.GroupChatRoomItem(
                        chatId = 1L,
                        title = "Group",
                        hasPermissions = true,
                        isActive = true,
                    )
                ),
                meetings = emptyList(),
            )

            val actual = underTest.getBulkChatRoomActions(setOf(1L)).map { it.testTag }

            assertThat(actual).containsExactly(
                CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG,
                CHAT_ROOM_ACTIONS_MUTE_TAG,
                CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
                CHAT_ROOM_ACTIONS_LEAVE_TAG,
            ).inOrder()
        }

    @Test
    fun `test that getBulkChatRoomActions returns only actions eligible for every selected chat`() =
        runTest {
            stubChatRooms(
                chats = listOf(
                    ChatRoomItem.GroupChatRoomItem(
                        chatId = 1L,
                        title = "Group",
                        hasPermissions = true,
                        isActive = true,
                    ),
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 2L,
                        title = "Chat",
                        hasPermissions = true,
                    ),
                ),
                meetings = emptyList(),
            )

            val actual = underTest.getBulkChatRoomActions(setOf(1L, 2L)).map { it.testTag }

            assertThat(actual).containsExactly(
                CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG,
                CHAT_ROOM_ACTIONS_MUTE_TAG,
                CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
            ).inOrder()
        }

    @Test
    fun `test that getBulkChatRoomActions returns empty for an empty selection`() = runTest {
        stubChatRooms()

        assertThat(underTest.getBulkChatRoomActions(emptySet())).isEmpty()
    }

    @Test
    fun `test that getBulkChatRoomActions returns empty for unknown chats`() = runTest {
        stubChatRooms()

        assertThat(underTest.getBulkChatRoomActions(setOf(999L))).isEmpty()
    }

    @Test
    fun `test that onBulkChatRoomActionSelected runs the picked action on every selected chat`() =
        runTest {
            stubChatRooms(
                chats = listOf(
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 1L,
                        title = "Chat",
                        hasPermissions = true,
                    ),
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 2L,
                        title = "Chat 2",
                        hasPermissions = true,
                    ),
                ),
                meetings = emptyList(),
            )

            underTest.onBulkChatRoomActionSelected(setOf(1L, 2L), CHAT_ROOM_ACTIONS_MUTE_TAG)
            advanceUntilIdle()

            verify(muteChatNotificationForChatRoomsUseCase)(
                chatIdList = listOf(1L, 2L),
                muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
            )
        }

    @Test
    fun `test that onBulkChatRoomActionSelected runs the action and queues its message when the ViewModel is cleared`() =
        runTest {
            val room = ChatRoomItem.IndividualChatRoomItem(
                chatId = 1L,
                title = "Chat",
                hasPermissions = true,
            )
            stubChatRooms(chats = listOf(room), meetings = emptyList())
            chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Chat" }
            val archiving = CompletableDeferred<Unit>()
            archiveChatUseCase.stub {
                onBlocking { invoke(eq(1L), any()) } doSuspendableAnswer { archiving.await() }
            }
            val viewModelStore = ViewModelStore().apply { put("chatList", underTest) }

            underTest.onBulkChatRoomActionSelected(setOf(1L), CHAT_ROOM_ACTIONS_ARCHIVE_TAG)
            advanceUntilIdle()
            viewModelStore.clear()
            archiving.complete(Unit)
            advanceUntilIdle()

            verify(archiveChatUseCase)(1L, true)
            verify(snackbarEventQueue).queueMessage(sharedR.string.success_archive_chat, "Chat")
        }

    @Test
    fun `test that onBulkChatRoomActionSelected stops resolving the selection when the ViewModel is cleared`() =
        runTest {
            var meetingsCancelled = false
            getChatsUseCase.stub {
                on {
                    invoke(eq(ChatRoomType.NON_MEETINGS), any(), any(), any(), any())
                } doAnswer {
                    flow {
                        emit(listOf(chatRoomItem))
                        awaitCancellation()
                    }
                }
                on {
                    invoke(eq(ChatRoomType.MEETINGS), any(), any(), any(), any())
                } doAnswer {
                    flow<List<ChatRoomItem>> {
                        try {
                            awaitCancellation()
                        } finally {
                            meetingsCancelled = true
                        }
                    }
                }
            }
            val viewModelStore = ViewModelStore().apply { put("chatList", underTest) }

            underTest.onBulkChatRoomActionSelected(setOf(1L), CHAT_ROOM_ACTIONS_ARCHIVE_TAG)
            advanceUntilIdle()
            viewModelStore.clear()
            advanceUntilIdle()

            assertThat(meetingsCancelled).isTrue()
            verifyNoInteractions(archiveChatUseCase)
        }

    @Test
    fun `test that onBulkChatRoomActionSelected does nothing for unknown chats`() = runTest {
        stubChatRooms()

        underTest.onBulkChatRoomActionSelected(setOf(999L), CHAT_ROOM_ACTIONS_MUTE_TAG)
        advanceUntilIdle()

        verifyNoInteractions(muteChatNotificationForChatRoomsUseCase)
    }

    @Test
    fun `test that onNewChatResult creates a 1to1 chat and emits its id when group settings are null`() =
        runTest {
            whenever(getContactHandleUseCase("alice@mega.co.nz")) doReturn 42L
            whenever(startConversationUseCase(isGroup = false, userHandles = listOf(42L))) doReturn 7L

            underTest.openChatEvents.test {
                underTest.onNewChatResult(
                    NewChatNavKey.NewChatResult(
                        emails = listOf("alice@mega.co.nz"),
                        groupSettings = null,
                    )
                )

                assertThat(awaitItem()).isEqualTo(7L)
            }
        }

    @Test
    fun `test that onNewChatResult does not emit when the contact handle is missing`() = runTest {
        whenever(getContactHandleUseCase("alice@mega.co.nz")) doReturn null

        underTest.openChatEvents.test {
            underTest.onNewChatResult(
                NewChatNavKey.NewChatResult(
                    emails = listOf("alice@mega.co.nz"),
                    groupSettings = null,
                )
            )
            advanceUntilIdle()

            expectNoEvents()
        }
        verifyNoInteractions(startConversationUseCase)
    }

    @Test
    fun `test that onNewChatResult creates a group chat and emits its id when group settings are present`() =
        runTest {
            val emails = listOf("alice@mega.co.nz", "bob@mega.co.nz")
            whenever(
                createGroupChatRoomUseCase(
                    emails = emails,
                    title = "Team",
                    isEkr = true,
                    addParticipants = false,
                    chatLink = false,
                )
            ) doReturn 99L

            underTest.openChatEvents.test {
                underTest.onNewChatResult(
                    NewChatNavKey.NewChatResult(
                        emails = emails,
                        groupSettings = NewChatNavKey.NewChatResult.GroupSettings(
                            title = "Team",
                            isEkr = true,
                            isChatLink = false,
                            allowAddParticipants = false,
                            imageUri = null,
                        ),
                    )
                )

                assertThat(awaitItem()).isEqualTo(99L)
            }
        }

    @Test
    fun `test that uiState exposes hasAnyContact when the user has contacts`() = runTest {
        stubChatRooms()
        stubHasAnyContact(hasAnyContact = true)

        underTest.uiState.test {
            assertThat(awaitDataState().hasAnyContact).isTrue()
        }
    }

    @Test
    fun `test that uiState exposes no contacts when the user has none`() = runTest {
        stubChatRooms()
        stubHasAnyContact(hasAnyContact = false)

        underTest.uiState.test {
            assertThat(awaitDataState().hasAnyContact).isFalse()
        }
    }

    @Test
    fun `test that uiState exposes no contacts when monitoring contacts fails`() = runTest {
        stubChatRooms()
        monitorHasAnyContactUseCase.stub {
            on { invoke() } doAnswer { flow<Boolean> { throw IllegalStateException("boom") } }
        }

        underTest.uiState.test {
            assertThat(awaitDataState().hasAnyContact).isFalse()
        }
    }

    private fun stubChatRooms(
        chats: List<ChatRoomItem> = listOf(chatRoomItem),
        meetings: List<ChatRoomItem> = listOf(meetingRoomItem),
    ) {
        getChatsUseCase.stub {
            on { invoke(eq(ChatRoomType.NON_MEETINGS), any(), any(), any(), any()) } doAnswer { invocation ->
                captureMappers(invocation)
                flow {
                    emit(chats)
                    awaitCancellation()
                }
            }
            on { invoke(eq(ChatRoomType.MEETINGS), any(), any(), any(), any()) } doAnswer { invocation ->
                captureMappers(invocation)
                flow {
                    emit(meetings)
                    awaitCancellation()
                }
            }
        }
        chatRoomUiItemMapper.stub {
            on { invoke(chatRoomItem) } doReturn chatUiItem
            on { invoke(meetingRoomItem) } doReturn meetingUiItem
        }
    }

    private fun stubHasAnyContact(hasAnyContact: Boolean) {
        monitorHasAnyContactUseCase.stub {
            on { invoke() } doReturn flowOf(hasAnyContact)
        }
    }

    private fun stubSearchableChats() {
        stubChatRooms(chats = listOf(aliceRoomItem, bobRoomItem), meetings = emptyList())
        chatRoomUiItemMapper.stub {
            on { invoke(aliceRoomItem) } doReturn aliceUiItem
            on { invoke(bobRoomItem) } doReturn bobUiItem
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureMappers(invocation: InvocationOnMock) {
        capturedLastMessage = invocation.getArgument<Any>(1) as suspend (Long) -> String
        capturedLastTimeMapper = invocation.getArgument<Any>(2) as (Long) -> String
        capturedMeetingTimeMapper = invocation.getArgument<Any>(3) as (Long, Long) -> String
        capturedHeaderTimeMapper =
            invocation.getArgument<Any>(4) as (ChatRoomItem, ChatRoomItem?) -> String?
    }

    private fun chatRoomUiItem(
        chatId: Long,
        title: String,
        lastMessage: String? = null,
        unreadCount: Int = 0,
    ) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = lastMessage,
        lastTimestampFormatted = null,
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = unreadCount,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
            placeholderText = null,
            filePath = null,
            color = null,
        ),
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )

    private fun ChatListTabState.items(): List<ChatRoomUiItem> =
        (this as ChatListTabState.Results).items

    private suspend fun ReceiveTurbine<ChatListUiState>.awaitDataState(): ChatListUiState.Data {
        var item = awaitItem()
        while (item !is ChatListUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    @Test
    fun `test that onMeetingActionSelected emits ScheduleMeeting when schedule is picked`() =
        runTest {
            stubChatRooms()

            underTest.meetingActionEvents.test {
                underTest.onMeetingActionSelected(MeetingAction.Schedule)

                assertThat(awaitItem()).isEqualTo(MeetingActionEvent.ScheduleMeeting)
            }
            verifyNoInteractions(isParticipatingInChatCallUseCase)
        }

    @Test
    fun `test that onMeetingActionSelected emits StartMeeting when not in a call`() = runTest {
        stubChatRooms()
        whenever(isParticipatingInChatCallUseCase()).thenReturn(false)

        underTest.meetingActionEvents.test {
            underTest.onMeetingActionSelected(MeetingAction.Start)

            assertThat(awaitItem()).isEqualTo(MeetingActionEvent.StartMeeting)
        }
    }

    @Test
    fun `test that onMeetingActionSelected emits StartBlockedByOngoingCall when already in a call`() =
        runTest {
            stubChatRooms()
            whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

            underTest.meetingActionEvents.test {
                underTest.onMeetingActionSelected(MeetingAction.Start)

                assertThat(awaitItem()).isEqualTo(MeetingActionEvent.StartBlockedByOngoingCall)
            }
        }

    @Test
    fun `test that onMeetingActionSelected emits JoinMeeting when not in a call`() = runTest {
        stubChatRooms()
        whenever(isParticipatingInChatCallUseCase()).thenReturn(false)

        underTest.meetingActionEvents.test {
            underTest.onMeetingActionSelected(MeetingAction.Join)

            assertThat(awaitItem()).isEqualTo(MeetingActionEvent.JoinMeeting)
        }
    }

    @Test
    fun `test that onMeetingActionSelected emits JoinBlockedByOngoingCall when already in a call`() =
        runTest {
            stubChatRooms()
            whenever(isParticipatingInChatCallUseCase()).thenReturn(true)

            underTest.meetingActionEvents.test {
                underTest.onMeetingActionSelected(MeetingAction.Join)

                assertThat(awaitItem()).isEqualTo(MeetingActionEvent.JoinBlockedByOngoingCall)
            }
        }

    @Test
    fun `test that onMeetingActionSelected emits StartMeeting when the call check fails`() =
        runTest {
            stubChatRooms()
            whenever(isParticipatingInChatCallUseCase())
                .thenThrow(RuntimeException("SDK unavailable"))

            underTest.meetingActionEvents.test {
                underTest.onMeetingActionSelected(MeetingAction.Start)

                assertThat(awaitItem()).isEqualTo(MeetingActionEvent.StartMeeting)
            }
        }

    private companion object {
        private val aliceRoomItem = ChatRoomItem.IndividualChatRoomItem(
            chatId = 10L,
            title = "Alice",
        )
        private val bobRoomItem = ChatRoomItem.IndividualChatRoomItem(
            chatId = 11L,
            title = "Bob",
        )
    }

    private val aliceUiItem = chatRoomUiItem(chatId = 10L, title = "Alice")
    private val bobUiItem = chatRoomUiItem(
        chatId = 11L,
        title = "Bob",
        lastMessage = "Dinner at 8?",
    )

    @Test
    fun `test that onBulkChatRoomActionSelected mutes every chat when the selection mixes muted and unmuted chats`() =
        runTest {
            stubChatRooms(
                chats = listOf(
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 1L,
                        title = "Unmuted",
                        hasPermissions = true,
                        isMuted = false,
                    ),
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 2L,
                        title = "Muted",
                        hasPermissions = true,
                        isMuted = true,
                    ),
                ),
                meetings = emptyList(),
            )

            val actions = underTest.getBulkChatRoomActions(setOf(1L, 2L))
            underTest.onBulkChatRoomActionSelected(setOf(1L, 2L), CHAT_ROOM_ACTIONS_MUTE_TAG)
            advanceUntilIdle()

            assertThat(actions.first { it.testTag == CHAT_ROOM_ACTIONS_MUTE_TAG }.label)
                .isEqualTo(sharedR.string.general_mute)
            verify(muteChatNotificationForChatRoomsUseCase)(
                chatIdList = listOf(1L, 2L),
                muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
            )
            verify(unmuteChatNotificationUseCase, never())(any())
        }

    @Test
    fun `test that getBulkChatRoomActions labels the action unmute when every selected chat is muted`() =
        runTest {
            stubChatRooms(
                chats = listOf(
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 1L,
                        title = "Muted",
                        hasPermissions = true,
                        isMuted = true,
                    ),
                    ChatRoomItem.IndividualChatRoomItem(
                        chatId = 2L,
                        title = "Also muted",
                        hasPermissions = true,
                        isMuted = true,
                    ),
                ),
                meetings = emptyList(),
            )

            val actions = underTest.getBulkChatRoomActions(setOf(1L, 2L))

            assertThat(actions.first { it.testTag == CHAT_ROOM_ACTIONS_MUTE_TAG }.label)
                .isEqualTo(sharedR.string.general_unmute)
        }
}
