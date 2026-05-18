package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.graphics.Color
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatListItem
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
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

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
    private val getContactHandleUseCase = mock<GetContactHandleUseCase>()
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
            getContactHandleUseCase,
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
        getContactHandleUseCase = getContactHandleUseCase,
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
        )

        chat.isGroup -> ChatExplorerUiItem.GroupChat(
            id = chat.chatId,
            title = chat.title,
            participants = 0,
            isSelected = false,
            isEnabled = true,
        )

        else -> ChatExplorerUiItem.OneToOneChat(
            id = chat.chatId,
            contactName = chat.title,
            primaryColor = Color.Unspecified,
            secondaryColor = null,
            userStatus = ChatStatus.Offline,
            isSelected = false,
            isEnabled = true,
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
    ) = ChatListItem(
        chatId = chatId,
        title = title,
        ownPrivilege = ChatRoomPermission.Standard,
        isGroup = isGroup,
        isNoteToSelf = isNoteToSelf,
        peerHandle = peerHandle,
        lastTimestamp = lastTimestamp,
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
    fun `test that monitorChatListItemUpdates triggers a reload`() = runTest {
        val first = chatItem(chatId = 1L, title = "First", lastTimestamp = 100L)
        val second = chatItem(chatId = 2L, title = "Second", lastTimestamp = 200L)
        val updates = MutableSharedFlow<ChatListItem>(extraBufferCapacity = 1)
        whenever(getActiveChatListItemsUseCase())
            .thenReturn(listOf(first))
            .thenReturn(listOf(first, second))
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
        }
    }

    @Test
    fun `test that archived noteToSelf is excluded from others`() = runTest {
        val archivedNote = chatItem(chatId = 9L, title = "Note", isNoteToSelf = true)
        stubChatLists(archived = listOf(archivedNote))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.others).isEmpty()
            verifyNoInteractions(chatExplorerUiItemMapper)
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
    fun `test that onContactsSelectedForGroupChat opens 1on1 chat when single email selected`() =
        runTest {
            stubChatLists()
            whenever(getContactHandleUseCase("alice@mega.nz")).thenReturn(42L)
            whenever(get1On1ChatIdUseCase(42L)).thenReturn(123L)
            val underTest = buildViewModel()

            underTest.onContactsSelectedForGroupChat(
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = listOf("alice@mega.nz"),
                    title = null,
                    isEkr = false,
                    allowAddParticipants = true,
                    isChatLink = false,
                )
            )

            verify(get1On1ChatIdUseCase).invoke(42L)
            verifyNoInteractions(createGroupChatRoomUseCase)
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
                )
            )

            verify(createGroupChatRoomUseCase).invoke(
                emails = listOf("alice@mega.nz", "bob@mega.nz"),
                title = "Team",
                isEkr = true,
                addParticipants = false,
                chatLink = true,
            )
            verifyNoInteractions(get1On1ChatIdUseCase)
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
                )
            )

            verifyNoInteractions(createGroupChatRoomUseCase)
            verifyNoInteractions(get1On1ChatIdUseCase)
            verifyNoInteractions(getContactHandleUseCase)
        }

}
