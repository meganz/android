package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.graphics.Color
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.GetActiveChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetArchivedChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.explorer.GetVisibleContactsWithoutChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

private val ChatExplorerUiState.Data.noteToSelf get() = items.noteToSelf
private val ChatExplorerUiState.Data.recents get() = items.recents
private val ChatExplorerUiState.Data.others get() = items.others

private const val NOTE_TO_SELF_TITLE = "Note to self"

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
internal class ChatExplorerViewModelTest {

    private val getActiveChatListItemsUseCase = mock<GetActiveChatListItemsUseCase>()
    private val getArchivedChatListItemsUseCase = mock<GetArchivedChatListItemsUseCase>()
    private val getVisibleContactsWithoutChatRoomUseCase =
        mock<GetVisibleContactsWithoutChatRoomUseCase>()
    private val getNoteToSelfChatUseCase = mock<GetNoteToSelfChatUseCase>()
    private val monitorChatListItemUpdates = mock<MonitorChatListItemUpdates>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()
    private val get1On1ChatIdUseCase = mock<Get1On1ChatIdUseCase>()
    private val sendTextMessageUseCase = mock<SendTextMessageUseCase>()
    private val chatExplorerUiItemMapper = mock<ChatExplorerUiItemMapper>()

    @AfterEach
    fun resetMocks() {
        reset(
            getActiveChatListItemsUseCase,
            getArchivedChatListItemsUseCase,
            getVisibleContactsWithoutChatRoomUseCase,
            getNoteToSelfChatUseCase,
            monitorChatListItemUpdates,
            createGroupChatRoomUseCase,
            get1On1ChatIdUseCase,
            sendTextMessageUseCase,
            chatExplorerUiItemMapper,
        )
    }

    private fun buildViewModel(): ChatExplorerViewModel = ChatExplorerViewModel(
        getActiveChatListItemsUseCase = getActiveChatListItemsUseCase,
        getArchivedChatListItemsUseCase = getArchivedChatListItemsUseCase,
        getVisibleContactsWithoutChatRoomUseCase = getVisibleContactsWithoutChatRoomUseCase,
        getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
        monitorChatListItemUpdates = monitorChatListItemUpdates,
        createGroupChatRoomUseCase = createGroupChatRoomUseCase,
        get1On1ChatIdUseCase = get1On1ChatIdUseCase,
        sendTextMessageUseCase = sendTextMessageUseCase,
        chatExplorerUiItemMapper = chatExplorerUiItemMapper,
    )

    private suspend fun stubChatLists(
        active: List<ChatListItem> = emptyList(),
        archived: List<ChatListItem> = emptyList(),
        contacts: List<UserContact> = emptyList(),
    ) {
        whenever(getActiveChatListItemsUseCase()).thenReturn(active)
        whenever(getArchivedChatListItemsUseCase()).thenReturn(archived)
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(contacts)
        whenever(monitorChatListItemUpdates()).thenReturn(emptyFlow())
    }

    private suspend fun stubMapperForChats(chats: List<ChatListItem>) {
        chats.forEach { chat ->
            whenever(chatExplorerUiItemMapper(eq(chat))).thenReturn(chatRow(chat))
        }
    }

    private suspend fun stubMapperForContacts(contacts: List<UserContact>) {
        contacts.forEach { contact ->
            whenever(chatExplorerUiItemMapper(eq(contact))).thenReturn(contactRow(contact))
        }
    }

    private fun chatRow(chat: ChatListItem): ChatExplorerUiItem = when {
        chat.isNoteToSelf -> ChatExplorerUiItem.NoteToSelf(
            id = chat.chatId,
            isHint = false,
            isSelected = false,
            isEnabled = true,
            isArchived = chat.isArchived,
            lastTimestamp = chat.lastTimestamp,
        )

        chat.isGroup -> ChatExplorerUiItem.GroupChat(
            id = chat.chatId,
            title = chat.title,
            participants = 0,
            isSelected = false,
            isEnabled = true,
            isArchived = chat.isArchived,
            lastTimestamp = chat.lastTimestamp,
        )

        else -> ChatExplorerUiItem.OneToOneChat(
            id = chat.chatId,
            contactName = chat.title,
            primaryColor = Color.Unspecified,
            secondaryColor = null,
            userStatus = ChatStatus.Offline,
            isSelected = false,
            isEnabled = true,
            isArchived = chat.isArchived,
            lastTimestamp = chat.lastTimestamp,
        )
    }

    private fun contactRow(contact: UserContact): ChatExplorerUiItem.Contact? {
        val user = contact.user ?: return null
        return ChatExplorerUiItem.Contact(
            id = user.handle,
            contactName = contact.contact?.fullName ?: user.email,
            contactEmail = user.email,
            primaryColor = Color.Unspecified,
            secondaryColor = null,
            userStatus = ChatStatus.Offline,
            isSelected = false,
            isEnabled = true,
        )
    }

    private suspend fun ReceiveTurbine<ChatExplorerUiState>.awaitData(): ChatExplorerUiState.Data {
        var item = awaitItem()
        while (item !is ChatExplorerUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private fun chatItem(
        chatId: Long,
        title: String,
        lastTimestamp: Long = chatId,
        isGroup: Boolean = false,
        isNoteToSelf: Boolean = false,
        peerHandle: Long = if (isGroup || isNoteToSelf) -1L else chatId,
        changes: ChatListItemChanges? = null,
        isArchived: Boolean = false,
    ) = ChatListItem(
        chatId = chatId,
        title = title,
        changes = changes,
        ownPrivilege = ChatRoomPermission.Standard,
        isGroup = isGroup,
        isNoteToSelf = isNoteToSelf,
        peerHandle = peerHandle,
        lastTimestamp = lastTimestamp,
        isArchived = isArchived,
    )

    private fun contactItem(
        handle: Long,
        email: String,
        fullName: String? = null,
    ) = UserContact(
        contact = fullName?.let { Contact(userId = handle, email = email, firstName = it) },
        user = User(
            handle = handle,
            email = email,
            visibility = UserVisibility.Visible,
            timestamp = 0L,
            userChanges = emptyList(),
        ),
    )

    @Test
    fun `test that noteToSelf is mapped via the ui item mapper`() = runTest {
        val note = chatItem(chatId = 1L, title = "Note", isNoteToSelf = true)
        stubChatLists(active = listOf(note))
        stubMapperForChats(listOf(note))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.noteToSelf).isEqualTo(chatRow(note))
            assertThat(data.recents).isEmpty()
            assertThat(data.others).isEmpty()
        }
    }

    @Test
    fun `test that searchResults exposes only the chats matching the query`() = runTest {
        val design = chatItem(chatId = 1L, title = "Design Team", isGroup = true)
        val marketing = chatItem(chatId = 2L, title = "Marketing", isGroup = true)
        stubChatLists(active = listOf(design, marketing))
        stubMapperForChats(listOf(design, marketing))
        val underTest = buildViewModel()

        underTest.onSearchQuery(ChatExplorerViewModel.ChatSearchInput("design", NOTE_TO_SELF_TITLE))

        underTest.uiState.test {
            advanceUntilIdle()
            val data = expectMostRecentItem() as ChatExplorerUiState.Data
            val matched = data.searchResults.recents + data.searchResults.others
            assertThat(matched.map { it.id }).containsExactly(1L)
        }
    }

    @Test
    fun `test that searchResults includes note-to-self when its title matches the query`() =
        runTest {
            val note = chatItem(chatId = 1L, title = "Note", isNoteToSelf = true)
            stubChatLists(active = listOf(note))
            stubMapperForChats(listOf(note))
            val underTest = buildViewModel()

            underTest.onSearchQuery(ChatExplorerViewModel.ChatSearchInput("note to", NOTE_TO_SELF_TITLE))

            underTest.uiState.test {
                advanceUntilIdle()
                val data = expectMostRecentItem() as ChatExplorerUiState.Data
                assertThat(data.searchResults.noteToSelf).isNotNull()
            }
        }

    @Test
    fun `test that searchResults excludes note-to-self when its title does not match the query`() =
        runTest {
            val note = chatItem(chatId = 1L, title = "Note", isNoteToSelf = true)
            stubChatLists(active = listOf(note))
            stubMapperForChats(listOf(note))
            val underTest = buildViewModel()

            underTest.onSearchQuery(ChatExplorerViewModel.ChatSearchInput("design", NOTE_TO_SELF_TITLE))

            underTest.uiState.test {
                advanceUntilIdle()
                val data = expectMostRecentItem() as ChatExplorerUiState.Data
                assertThat(data.searchResults.noteToSelf).isNull()
            }
        }

    @Test
    fun `test that a blank query exposes no search results`() = runTest {
        val design = chatItem(chatId = 1L, title = "Design Team", isGroup = true)
        stubChatLists(active = listOf(design))
        stubMapperForChats(listOf(design))
        val underTest = buildViewModel()

        underTest.onSearchQuery(ChatExplorerViewModel.ChatSearchInput("", NOTE_TO_SELF_TITLE))

        underTest.uiState.test {
            advanceUntilIdle()
            val data = expectMostRecentItem() as ChatExplorerUiState.Data
            assertThat(data.searchResults.isEmpty).isTrue()
        }
    }

    @Test
    fun `test that recents preserve lastTimestamp descending order`() = runTest {
        val older = chatItem(chatId = 1L, title = "Alice", lastTimestamp = 100L)
        val newer = chatItem(chatId = 2L, title = "Bob", lastTimestamp = 200L)
        stubChatLists(active = listOf(older, newer))
        stubMapperForChats(listOf(older, newer))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.recents.map { it.id }).containsExactly(2L, 1L).inOrder()
        }
    }

    @Test
    fun `test that recents are limited to five most recent chats`() = runTest {
        val chats = (1..7).map { i ->
            chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong())
        }
        stubChatLists(active = chats)
        stubMapperForChats(chats)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.recents).hasSize(5)
            assertThat(data.recents.map { it.id })
                .containsExactly(7L, 6L, 5L, 4L, 3L).inOrder()
        }
    }

    @Test
    fun `test that others merges overflow archived and contacts sorted alphabetically`() = runTest {
        val recents = (1..5).map { i ->
            chatItem(chatId = i.toLong(), title = "Recent $i", lastTimestamp = (10 - i).toLong())
        }
        val overflow =
            chatItem(chatId = 100L, title = "Zebra group", isGroup = true, lastTimestamp = 0L)
        val archivedApple = chatItem(chatId = 200L, title = "Apple group", isGroup = true)
        val archivedMango = chatItem(chatId = 201L, title = "Mango group", isGroup = true)
        val contact = contactItem(handle = 300L, email = "carl@mega.nz", fullName = "Carl")
        stubChatLists(
            active = recents + overflow,
            archived = listOf(archivedMango, archivedApple),
            contacts = listOf(contact),
        )
        stubMapperForChats(recents + overflow + archivedApple + archivedMango)
        stubMapperForContacts(listOf(contact))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            val titles = data.others.map { item ->
                when (item) {
                    is ChatExplorerUiItem.GroupChat -> item.title
                    is ChatExplorerUiItem.Contact -> item.contactName.orEmpty()
                    else -> error("Unexpected variant ${item::class.simpleName}")
                }
            }
            assertThat(titles)
                .containsExactly("Apple group", "Carl", "Mango group", "Zebra group").inOrder()
        }
    }

    @Test
    fun `test that update for new chat is prepended to recents`() = runTest {
        val first = chatItem(chatId = 1L, title = "First", lastTimestamp = 100L)
        val second = chatItem(
            chatId = 2L,
            title = "Second",
            lastTimestamp = 200L,
            changes = ChatListItemChanges.LastTS,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(first))
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(first, second))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val firstData = awaitData()
            assertThat(firstData.recents.map { it.id }).containsExactly(1L)

            updates.emit(second)
            val secondData = awaitItem() as ChatExplorerUiState.Data
            assertThat(secondData.recents.map { it.id }).containsExactly(2L, 1L).inOrder()
            verify(getActiveChatListItemsUseCase, times(1)).invoke()
        }
    }

    @Test
    fun `test that update for existing chat in recents replaces it in place`() = runTest {
        val original = chatItem(
            chatId = 1L,
            title = "Original",
            isGroup = true,
            lastTimestamp = 100L,
        )
        val renamed = chatItem(
            chatId = 1L,
            title = "Renamed",
            isGroup = true,
            lastTimestamp = 100L,
            changes = ChatListItemChanges.Title,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(original))
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(original, renamed))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat((before.recents.single() as ChatExplorerUiItem.GroupChat).title)
                .isEqualTo("Original")

            updates.emit(renamed)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents).hasSize(1)
            assertThat((after.recents.single() as ChatExplorerUiItem.GroupChat).title)
                .isEqualTo("Renamed")
            verify(getActiveChatListItemsUseCase, times(1)).invoke()
        }
    }

    @Test
    fun `test that update with non in place change is ignored`() = runTest {
        val first = chatItem(
            chatId = 1L,
            title = "Renamed",
            isGroup = true,
            changes = ChatListItemChanges.Title,
        )
        val ignored = chatItem(
            chatId = 2L,
            title = "Ignored",
            isGroup = true,
            changes = ChatListItemChanges.UnreadCount,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 2)
        whenever(getActiveChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(first, ignored))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val empty = awaitData()
            assertThat(empty.recents).isEmpty()

            updates.emit(ignored)
            updates.emit(first)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id }).containsExactly(1L)
            assertThat(after.others).isEmpty()
        }
    }

    @Test
    fun `test that new chat overflowing recents pushes oldest to others`() = runTest {
        val recents = (1..5).map { i ->
            chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong())
        }
        val newChat = chatItem(
            chatId = 100L,
            title = "Zebra",
            isGroup = true,
            lastTimestamp = 500L,
            changes = ChatListItemChanges.LastTS,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(recents)
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(recents + newChat)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.recents.map { it.id })
                .containsExactly(5L, 4L, 3L, 2L, 1L).inOrder()
            assertThat(before.others).isEmpty()

            updates.emit(newChat)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id })
                .containsExactly(100L, 5L, 4L, 3L, 2L).inOrder()
            assertThat(after.others.map { it.id }).containsExactly(1L)
        }
    }

    @Test
    fun `test that sequential updates compound over the latest state`() = runTest {
        val firstChat = chatItem(
            chatId = 1L,
            title = "First",
            lastTimestamp = 100L,
            changes = ChatListItemChanges.LastTS,
        )
        val secondChat = chatItem(
            chatId = 2L,
            title = "Second",
            lastTimestamp = 200L,
            changes = ChatListItemChanges.LastTS,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 2)
        whenever(getActiveChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(firstChat, secondChat))
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()

            updates.emit(firstChat)
            val afterFirst = awaitItem() as ChatExplorerUiState.Data
            assertThat(afterFirst.recents.map { it.id }).containsExactly(1L)

            updates.emit(secondChat)
            val afterSecond = awaitItem() as ChatExplorerUiState.Data
            assertThat(afterSecond.recents.map { it.id }).containsExactly(2L, 1L).inOrder()
        }
    }

    @Test
    fun `test that archive update moves a recents chat into others`() = runTest {
        val first = chatItem(chatId = 1L, title = "Alpha", isGroup = true, lastTimestamp = 100L)
        val second = chatItem(chatId = 2L, title = "Beta", isGroup = true, lastTimestamp = 200L)
        val archived = chatItem(
            chatId = 1L,
            title = "Alpha",
            isGroup = true,
            lastTimestamp = 100L,
            changes = ChatListItemChanges.Archive,
            isArchived = true,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(first, second))
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(first, second, archived))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.recents.map { it.id }).containsExactly(2L, 1L).inOrder()
            assertThat(before.others).isEmpty()

            updates.emit(archived)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id }).containsExactly(2L)
            assertThat(after.others.map { it.id }).containsExactly(1L)
        }
    }

    @Test
    fun `test that archive from recents promotes the highest-timestamp active overflow`() = runTest {
        val recents = (1..5).map { i ->
            chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong() + 10)
        }
        val overflowZ =
            chatItem(chatId = 100L, title = "Zebra", isGroup = true, lastTimestamp = 9L)
        val overflowA =
            chatItem(chatId = 101L, title = "Aardvark", isGroup = true, lastTimestamp = 5L)
        val archivedOldest = chatItem(
            chatId = 1L,
            title = "Chat 1",
            lastTimestamp = 11L,
            changes = ChatListItemChanges.Archive,
            isArchived = true,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase())
            .thenReturn(recents + overflowZ + overflowA)
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(recents + overflowZ + overflowA + archivedOldest)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.recents.map { it.id })
                .containsExactly(5L, 4L, 3L, 2L, 1L).inOrder()
            assertThat(before.others.map { it.id }).containsExactly(101L, 100L)

            updates.emit(archivedOldest)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id })
                .containsExactly(5L, 4L, 3L, 2L, 100L).inOrder()
            assertThat(after.others.map { it.id }).containsExactly(101L, 1L)
        }
    }

    @Test
    fun `test that archive from recents skips archived items when picking promotion`() = runTest {
        val recents = (1..5).map { i ->
            chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong() + 10)
        }
        val archivedAlpha = chatItem(
            chatId = 200L,
            title = "Aardvark archived",
            isGroup = true,
            isArchived = true,
        )
        val activeOverflow = chatItem(
            chatId = 201L,
            title = "Zebra active",
            isGroup = true,
            lastTimestamp = 5L,
        )
        val archivingFromRecents = chatItem(
            chatId = 1L,
            title = "Chat 1",
            lastTimestamp = 11L,
            changes = ChatListItemChanges.Archive,
            isArchived = true,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(recents + activeOverflow)
        whenever(getArchivedChatListItemsUseCase()).thenReturn(listOf(archivedAlpha))
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(recents + activeOverflow + archivedAlpha + archivingFromRecents)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.others.map { it.id }).containsExactly(200L, 201L)

            updates.emit(archivingFromRecents)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id })
                .containsExactly(5L, 4L, 3L, 2L, 201L).inOrder()
            assertThat(after.others.map { it.id }).containsExactly(200L, 1L)
        }
    }

    @Test
    fun `test that archive update for a new chat inserts it into others`() = runTest {
        val present = chatItem(chatId = 1L, title = "Alpha", isGroup = true, lastTimestamp = 100L)
        val newArchived = chatItem(
            chatId = 9L,
            title = "Aardvark",
            isGroup = true,
            lastTimestamp = 50L,
            changes = ChatListItemChanges.Archive,
            isArchived = true,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(present))
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(present, newArchived))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.recents.map { it.id }).containsExactly(1L)
            assertThat(before.others).isEmpty()

            updates.emit(newArchived)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.recents.map { it.id }).containsExactly(1L)
            assertThat(after.others.map { it.id }).containsExactly(9L)
        }
    }

    @Test
    fun `test that unarchive update moves an others chat into recents when there is room`() =
        runTest {
            val active = chatItem(chatId = 1L, title = "Active", isGroup = true, lastTimestamp = 100L)
            val archivedChat = chatItem(
                chatId = 2L,
                title = "Archived",
                isGroup = true,
                lastTimestamp = 50L,
                isArchived = true,
            )
            val unarchived = chatItem(
                chatId = 2L,
                title = "Archived",
                isGroup = true,
                lastTimestamp = 50L,
                changes = ChatListItemChanges.Archive,
                isArchived = false,
            )
            val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
            whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(active))
            whenever(getArchivedChatListItemsUseCase()).thenReturn(listOf(archivedChat))
            whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
            whenever(monitorChatListItemUpdates()).thenReturn(updates)
            stubMapperForChats(listOf(active, archivedChat, unarchived))
            val underTest = buildViewModel()

            underTest.uiState.test {
                val before = awaitData()
                assertThat(before.recents.map { it.id }).containsExactly(1L)
                assertThat(before.others.map { it.id }).containsExactly(2L)

                updates.emit(unarchived)
                val after = awaitItem() as ChatExplorerUiState.Data
                assertThat(after.recents.map { it.id }).containsExactly(1L, 2L).inOrder()
                assertThat(after.others).isEmpty()
            }
        }

    @Test
    fun `test that unarchive update keeps chat in others when its timestamp is below recents minimum`() =
        runTest {
            val recents = (1..5).map { i ->
                chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong() + 100)
            }
            val archivedChat = chatItem(
                chatId = 9L,
                title = "Archived",
                isGroup = true,
                lastTimestamp = 50L,
                isArchived = true,
            )
            val unarchived = chatItem(
                chatId = 9L,
                title = "Reactivated",
                isGroup = true,
                lastTimestamp = 50L,
                changes = ChatListItemChanges.Archive,
                isArchived = false,
            )
            val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
            whenever(getActiveChatListItemsUseCase()).thenReturn(recents)
            whenever(getArchivedChatListItemsUseCase()).thenReturn(listOf(archivedChat))
            whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
            whenever(monitorChatListItemUpdates()).thenReturn(updates)
            stubMapperForChats(recents + archivedChat + unarchived)
            val underTest = buildViewModel()

            underTest.uiState.test {
                val before = awaitData()
                assertThat(before.recents).hasSize(5)
                assertThat((before.others.single() as ChatExplorerUiItem.GroupChat).title)
                    .isEqualTo("Archived")

                updates.emit(unarchived)
                val after = awaitItem() as ChatExplorerUiState.Data
                assertThat(after.recents.map { it.id })
                    .containsExactly(5L, 4L, 3L, 2L, 1L).inOrder()
                assertThat(after.others).hasSize(1)
                assertThat((after.others.single() as ChatExplorerUiItem.GroupChat).title)
                    .isEqualTo("Reactivated")
            }
        }

    @Test
    fun `test that unarchive update promotes the chat when its timestamp beats recents minimum`() =
        runTest {
            val recents = (1..5).map { i ->
                chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = i.toLong())
            }
            val archivedChat = chatItem(
                chatId = 9L,
                title = "Archived",
                isGroup = true,
                lastTimestamp = 50L,
                isArchived = true,
            )
            val unarchived = chatItem(
                chatId = 9L,
                title = "Reactivated",
                isGroup = true,
                lastTimestamp = 50L,
                changes = ChatListItemChanges.Archive,
                isArchived = false,
            )
            val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
            whenever(getActiveChatListItemsUseCase()).thenReturn(recents)
            whenever(getArchivedChatListItemsUseCase()).thenReturn(listOf(archivedChat))
            whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
            whenever(monitorChatListItemUpdates()).thenReturn(updates)
            stubMapperForChats(recents + archivedChat + unarchived)
            val underTest = buildViewModel()

            underTest.uiState.test {
                val before = awaitData()
                assertThat(before.recents.map { it.id })
                    .containsExactly(5L, 4L, 3L, 2L, 1L).inOrder()

                updates.emit(unarchived)
                val after = awaitItem() as ChatExplorerUiState.Data
                assertThat(after.recents.map { it.id })
                    .containsExactly(9L, 5L, 4L, 3L, 2L).inOrder()
                assertThat(after.others.map { it.id }).containsExactly(1L)
            }
        }

    @Test
    fun `test that update for existing chat in others replaces it in place`() = runTest {
        val recents = (1..5).map { i ->
            chatItem(chatId = i.toLong(), title = "Chat $i", lastTimestamp = (10 - i).toLong())
        }
        val overflow = chatItem(chatId = 100L, title = "Zebra", isGroup = true, lastTimestamp = 0L)
        val renamed = chatItem(
            chatId = 100L,
            title = "Aardvark",
            isGroup = true,
            lastTimestamp = 0L,
            changes = ChatListItemChanges.Title,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(recents + overflow)
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(recents + overflow + renamed)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat((before.others.single() as ChatExplorerUiItem.GroupChat).title)
                .isEqualTo("Zebra")

            updates.emit(renamed)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.others).hasSize(1)
            assertThat((after.others.single() as ChatExplorerUiItem.GroupChat).title)
                .isEqualTo("Aardvark")
        }
    }

    @Test
    fun `test that archived noteToSelf is placed in others`() = runTest {
        val archivedNote = chatItem(
            chatId = 9L,
            title = "Note",
            isNoteToSelf = true,
            isArchived = true,
        )
        stubChatLists(archived = listOf(archivedNote))
        stubMapperForChats(listOf(archivedNote))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.noteToSelf).isNull()
            assertThat(data.others.single()).isInstanceOf(ChatExplorerUiItem.NoteToSelf::class.java)
            assertThat(data.others.single().id).isEqualTo(9L)
        }
    }

    @Test
    fun `test that archiving the active noteToSelf moves it from slot into others`() = runTest {
        val activeNote = chatItem(chatId = 9L, title = "Note", isNoteToSelf = true)
        val archivedNote = chatItem(
            chatId = 9L,
            title = "Note",
            isNoteToSelf = true,
            isArchived = true,
            changes = ChatListItemChanges.Archive,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(listOf(activeNote))
        whenever(getArchivedChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(activeNote, archivedNote))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.noteToSelf).isNotNull()
            assertThat(before.others).isEmpty()

            updates.emit(archivedNote)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.noteToSelf).isNull()
            assertThat(after.others.single()).isInstanceOf(ChatExplorerUiItem.NoteToSelf::class.java)
            assertThat(after.others.single().isArchived).isTrue()
        }
    }

    @Test
    fun `test that unarchiving a noteToSelf in others moves it back into the slot`() = runTest {
        val archivedNote = chatItem(
            chatId = 9L,
            title = "Note",
            isNoteToSelf = true,
            isArchived = true,
        )
        val unarchivedNote = chatItem(
            chatId = 9L,
            title = "Note",
            isNoteToSelf = true,
            isArchived = false,
            changes = ChatListItemChanges.Archive,
        )
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase()).thenReturn(emptyList())
        whenever(getArchivedChatListItemsUseCase()).thenReturn(listOf(archivedNote))
        whenever(getVisibleContactsWithoutChatRoomUseCase()).thenReturn(emptyList())
        whenever(monitorChatListItemUpdates()).thenReturn(updates)
        stubMapperForChats(listOf(archivedNote, unarchivedNote))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val before = awaitData()
            assertThat(before.noteToSelf).isNull()
            assertThat(before.others).hasSize(1)

            updates.emit(unarchivedNote)
            val after = awaitItem() as ChatExplorerUiState.Data
            assertThat(after.noteToSelf).isNotNull()
            assertThat(after.noteToSelf!!.isArchived).isFalse()
            assertThat(after.others).isEmpty()
        }
    }

    @Test
    fun `test that contacts dropped by mapper are excluded from others`() = runTest {
        val anonymous = UserContact(contact = null, user = null)
        stubChatLists(contacts = listOf(anonymous))
        whenever(chatExplorerUiItemMapper(eq(anonymous))).thenReturn(null)
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.others).isEmpty()
        }
    }

    @Test
    fun `test that onContactsSelectedForGroupChat creates group when single email selected`() =
        runTest {
            stubChatLists()
            whenever(
                createGroupChatRoomUseCase(
                    emails = listOf("alice@mega.nz"),
                    title = null,
                    isEkr = false,
                    addParticipants = true,
                    chatLink = false,
                )
            ).thenReturn(123L)
            val underTest = buildViewModel()

            underTest.onContactsSelectedForGroupChat(
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = listOf("alice@mega.nz"),
                    title = null,
                    isEkr = false,
                    allowAddParticipants = true,
                    isChatLink = false,
                    imageUri = null,
                )
            )

            verify(createGroupChatRoomUseCase).invoke(
                emails = listOf("alice@mega.nz"),
                title = null,
                isEkr = false,
                addParticipants = true,
                chatLink = false,
            )
        }

    @Test
    fun `test that onContactsSelectedForGroupChat creates group when multiple emails selected`() =
        runTest {
            stubChatLists()
            val underTest = buildViewModel()

            underTest.onContactsSelectedForGroupChat(
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = listOf("alice@mega.nz", "bob@mega.nz"),
                    title = "Team",
                    isEkr = true,
                    allowAddParticipants = false,
                    isChatLink = true,
                    imageUri = null,
                )
            )

            verify(createGroupChatRoomUseCase).invoke(
                emails = listOf("alice@mega.nz", "bob@mega.nz"),
                title = "Team",
                isEkr = true,
                addParticipants = false,
                chatLink = true,
            )
        }

    @Test
    fun `test that onContactsSelectedForGroupChat does nothing when no emails selected`() =
        runTest {
            stubChatLists()
            val underTest = buildViewModel()

            underTest.onContactsSelectedForGroupChat(
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = emptyList(),
                    title = null,
                    isEkr = false,
                    allowAddParticipants = true,
                    isChatLink = false,
                    imageUri = null,
                )
            )

            verifyNoInteractions(createGroupChatRoomUseCase)
        }

    @Test
    fun `test that onContactsSelectedForGroupChat emits newChatCreatedEvent on success`() =
        runTest {
            stubChatLists()
            stubMapperForChats(emptyList())
            whenever(
                createGroupChatRoomUseCase(
                    emails = listOf("alice@mega.nz", "bob@mega.nz"),
                    title = "Team",
                    isEkr = false,
                    addParticipants = true,
                    chatLink = false,
                )
            ).thenReturn(456L)
            val underTest = buildViewModel()

            underTest.uiState.test {
                val initial = awaitData()
                assertThat(initial.newChatCreatedEvent).isEqualTo(consumed())

                underTest.onContactsSelectedForGroupChat(
                    CreateGroupChatNavKey.NewGroupChatResult(
                        emails = listOf("alice@mega.nz", "bob@mega.nz"),
                        title = "Team",
                        isEkr = false,
                        allowAddParticipants = true,
                        isChatLink = false,
                        imageUri = null,
                    )
                )

                val triggered = awaitItem() as ChatExplorerUiState.Data
                assertThat(triggered.newChatCreatedEvent).isEqualTo(triggered(456L))
            }
        }

    @Test
    fun `test that onNewChatCreatedConsumed clears the event`() = runTest {
        stubChatLists()
        stubMapperForChats(emptyList())
        whenever(
            createGroupChatRoomUseCase(
                emails = listOf("alice@mega.nz"),
                title = null,
                isEkr = false,
                addParticipants = true,
                chatLink = false,
            )
        ).thenReturn(789L)
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()

            underTest.onContactsSelectedForGroupChat(
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = listOf("alice@mega.nz"),
                    title = null,
                    isEkr = false,
                    allowAddParticipants = true,
                    isChatLink = false,
                    imageUri = null,
                )
            )
            val triggered = awaitItem() as ChatExplorerUiState.Data
            assertThat(triggered.newChatCreatedEvent).isEqualTo(triggered(789L))

            underTest.onNewChatCreatedConsumed()
            val consumed = awaitItem() as ChatExplorerUiState.Data
            assertThat(consumed.newChatCreatedEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that prepareChatsForSharing emits chat ids unchanged for existing chats`() = runTest {
        val chatA = chatItem(chatId = 10L, title = "Alice", isGroup = false)
        val chatB = chatItem(chatId = 11L, title = "Group", isGroup = true)
        stubChatLists(active = listOf(chatA, chatB))
        stubMapperForChats(listOf(chatA, chatB))
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()
            underTest.prepareChatsForSharing(listOf(10L, 11L), message = null)
            val event = awaitChatsReadyToShare()
            assertThat(event).containsExactly(10L, 11L).inOrder()
            verifyNoInteractions(get1On1ChatIdUseCase)
            verifyNoInteractions(sendTextMessageUseCase)
        }
    }

    @Test
    fun `test that prepareChatsForSharing resolves contact rows via Get1On1ChatIdUseCase`() =
        runTest {
            val contact = contactItem(handle = 42L, email = "alice@mega.nz", fullName = "Alice")
            stubChatLists(contacts = listOf(contact))
            stubMapperForContacts(listOf(contact))
            whenever(get1On1ChatIdUseCase(42L)).thenReturn(999L)
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitData()
                underTest.prepareChatsForSharing(listOf(42L), message = null)
                val event = awaitChatsReadyToShare()
                assertThat(event).containsExactly(999L)
                verify(get1On1ChatIdUseCase).invoke(42L)
            }
        }

    @Test
    fun `test that prepareChatsForSharing drops contacts whose 1to1 chat creation fails`() =
        runTest {
            val contact = contactItem(handle = 7L, email = "bob@mega.nz", fullName = "Bob")
            val chat = chatItem(chatId = 8L, title = "Carol", isGroup = false)
            stubChatLists(active = listOf(chat), contacts = listOf(contact))
            stubMapperForChats(listOf(chat))
            stubMapperForContacts(listOf(contact))
            whenever(get1On1ChatIdUseCase(7L)).thenThrow(RuntimeException("boom"))
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitData()
                underTest.prepareChatsForSharing(listOf(7L, 8L), message = null)
                val event = awaitChatsReadyToShare()
                assertThat(event).containsExactly(8L)
            }
        }

    @Test
    fun `test that prepareChatsForSharing does nothing for empty selection`() = runTest {
        stubChatLists()
        val underTest = buildViewModel()

        underTest.uiState.test {
            val initial = awaitData()
            assertThat(initial.chatsReadyToShareEvent).isEqualTo(consumed())

            underTest.prepareChatsForSharing(emptyList(), message = null)
            advanceUntilIdle()

            expectNoEvents()
            verifyNoInteractions(get1On1ChatIdUseCase)
            verifyNoInteractions(sendTextMessageUseCase)
        }
    }

    @Test
    fun `test that prepareChatsForSharing sends the message to each resolved chat id`() = runTest {
        val chatA = chatItem(chatId = 10L, title = "Alice", isGroup = false)
        val chatB = chatItem(chatId = 11L, title = "Group", isGroup = true)
        stubChatLists(active = listOf(chatA, chatB))
        stubMapperForChats(listOf(chatA, chatB))
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()
            underTest.prepareChatsForSharing(listOf(10L, 11L), message = "hello")
            val event = awaitChatsReadyToShare()
            assertThat(event).containsExactly(10L, 11L).inOrder()
            verify(sendTextMessageUseCase).invoke(chatId = 10L, message = "hello")
            verify(sendTextMessageUseCase).invoke(chatId = 11L, message = "hello")
        }
    }

    @Test
    fun `test that prepareChatsForSharing does not send the message when it is null`() = runTest {
        val chat = chatItem(chatId = 10L, title = "Alice", isGroup = false)
        stubChatLists(active = listOf(chat))
        stubMapperForChats(listOf(chat))
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()
            underTest.prepareChatsForSharing(listOf(10L), message = null)
            awaitChatsReadyToShare()
            verify(sendTextMessageUseCase, never()).invoke(any(), any())
        }
    }

    @Test
    fun `test that onChatsReadyToShareConsumed clears the chatsReadyToShareEvent`() = runTest {
        val chat = chatItem(chatId = 10L, title = "Alice", isGroup = false)
        stubChatLists(active = listOf(chat))
        stubMapperForChats(listOf(chat))
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitData()
            underTest.prepareChatsForSharing(listOf(10L), message = null)
            val triggeredState = awaitItem() as ChatExplorerUiState.Data
            assertThat(triggeredState.chatsReadyToShareEvent).isEqualTo(triggered(listOf(10L)))

            underTest.onChatsReadyToShareConsumed()
            val consumedState = awaitItem() as ChatExplorerUiState.Data
            assertThat(consumedState.chatsReadyToShareEvent).isEqualTo(consumed())
        }
    }

    private suspend fun ReceiveTurbine<ChatExplorerUiState>.awaitChatsReadyToShare(): List<Long> {
        while (true) {
            val state = awaitItem() as ChatExplorerUiState.Data
            val event = state.chatsReadyToShareEvent
            if (event is StateEventWithContentTriggered<*>) {
                @Suppress("UNCHECKED_CAST")
                return (event as StateEventWithContentTriggered<List<Long>>).content
            }
        }
    }
}
