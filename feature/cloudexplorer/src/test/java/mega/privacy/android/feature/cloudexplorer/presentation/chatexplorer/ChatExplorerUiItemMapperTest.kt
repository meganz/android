package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.GetCombinedChatRoomUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarSecondaryColorUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatExplorerUiItemMapperTest {

    private val getUserAvatarColorUseCase = mock<GetUserAvatarColorUseCase>()
    private val getUserAvatarSecondaryColorUseCase = mock<GetUserAvatarSecondaryColorUseCase>()
    private val getUserOnlineStatusByHandleUseCase = mock<GetUserOnlineStatusByHandleUseCase>()
    private val getCombinedChatRoomUseCase = mock<GetCombinedChatRoomUseCase>()

    private lateinit var underTest: ChatExplorerUiItemMapper

    @BeforeEach
    fun setUp() {
        underTest = ChatExplorerUiItemMapper(
            getUserAvatarColorUseCase = getUserAvatarColorUseCase,
            getUserAvatarSecondaryColorUseCase = getUserAvatarSecondaryColorUseCase,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            getCombinedChatRoomUseCase = getCombinedChatRoomUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getUserAvatarColorUseCase,
            getUserAvatarSecondaryColorUseCase,
            getUserOnlineStatusByHandleUseCase,
            getCombinedChatRoomUseCase,
        )
    }

    private fun chatItem(
        chatId: Long,
        title: String,
        isGroup: Boolean = false,
        isNoteToSelf: Boolean = false,
        peerHandle: Long = if (isGroup || isNoteToSelf) -1L else chatId,
        ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
    ) = ChatListItem(
        chatId = chatId,
        title = title,
        ownPrivilege = ownPrivilege,
        isGroup = isGroup,
        isNoteToSelf = isNoteToSelf,
        peerHandle = peerHandle,
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
    fun `test that note to self chat is mapped to a NoteToSelf ui item`() = runTest {
        val result = underTest(chatItem(chatId = 1L, title = "Note", isNoteToSelf = true))

        val note = result as ChatExplorerUiItem.NoteToSelf
        assertThat(note.id).isEqualTo(1L)
        assertThat(note.isHint).isFalse()
        assertThat(note.isEnabled).isTrue()
    }

    @Test
    fun `test that one to one chat is mapped with avatar color and online status`() = runTest {
        val handle = 100L
        whenever(getUserAvatarColorUseCase(handle)).thenReturn(0xFFE65100.toInt())
        whenever(getUserAvatarSecondaryColorUseCase(handle)).thenReturn(0xFFFFB74D.toInt())
        whenever(getUserOnlineStatusByHandleUseCase(handle)).thenReturn(UserChatStatus.Online)

        val result = underTest(chatItem(chatId = 1L, title = "Alice", peerHandle = handle))

        val row = result as ChatExplorerUiItem.OneToOneChat
        assertThat(row.id).isEqualTo(1L)
        assertThat(row.contactName).isEqualTo("Alice")
        assertThat(row.primaryColor).isEqualTo(Color(0xFFE65100.toInt()))
        assertThat(row.secondaryColor).isEqualTo(Color(0xFFFFB74D.toInt()))
        assertThat(row.userStatus).isEqualTo(ChatStatus.Online)
        assertThat(row.isEnabled).isTrue()
    }

    @Test
    fun `test that group chat is mapped to GroupChat with participant count`() = runTest {
        whenever(getCombinedChatRoomUseCase(10L)).thenReturn(
            CombinedChatRoom(
                chatId = 10L,
                title = "Design Team",
                peerCount = 5L,
                ownPrivilege = ChatRoomPermission.Standard,
                isGroup = true,
                isActive = true,
            ),
        )

        val result = underTest(chatItem(chatId = 10L, title = "Design Team", isGroup = true))

        val row = result as ChatExplorerUiItem.GroupChat
        assertThat(row.title).isEqualTo("Design Team")
        assertThat(row.participants).isEqualTo(5)
    }

    @Test
    fun `test that group chat is mapped to Meeting when combined room is a meeting`() = runTest {
        whenever(getCombinedChatRoomUseCase(11L)).thenReturn(
            CombinedChatRoom(
                chatId = 11L,
                title = "Weekly sync",
                peerCount = 3L,
                ownPrivilege = ChatRoomPermission.Standard,
                isGroup = true,
                isMeeting = true,
                isActive = true,
            ),
        )

        val result = underTest(chatItem(chatId = 11L, title = "Weekly sync", isGroup = true))

        assertThat(result).isInstanceOf(ChatExplorerUiItem.Meeting::class.java)
    }

    @Test
    fun `test that participant count falls back to zero when combined chat room lookup fails`() =
        runTest {
            whenever(getCombinedChatRoomUseCase(12L)).thenReturn(null)

            val result = underTest(chatItem(chatId = 12L, title = "Empty", isGroup = true))

            val row = result as ChatExplorerUiItem.GroupChat
            assertThat(row.participants).isEqualTo(0)
        }

    @Test
    fun `test that chat is disabled when ownPrivilege is below Standard`() = runTest {
        whenever(getCombinedChatRoomUseCase(20L)).thenReturn(null)

        val result = underTest(
            chatItem(
                chatId = 20L,
                title = "ReadOnly Group",
                isGroup = true,
                ownPrivilege = ChatRoomPermission.ReadOnly,
            ),
        )

        val row = result as ChatExplorerUiItem.GroupChat
        assertThat(row.isEnabled).isFalse()
    }

    @Test
    fun `test that online status falls back to Offline when lookup fails`() = runTest {
        val handle = 100L
        whenever(getUserAvatarColorUseCase(handle)).thenReturn(0)
        whenever(getUserOnlineStatusByHandleUseCase(handle))
            .thenThrow(RuntimeException("boom"))

        val result = underTest(chatItem(chatId = 1L, title = "Alice", peerHandle = handle))

        val row = result as ChatExplorerUiItem.OneToOneChat
        assertThat(row.userStatus).isEqualTo(ChatStatus.Offline)
    }

    @Test
    fun `test that avatar primary color falls back to Unspecified when lookup fails`() = runTest {
        val handle = 100L
        whenever(getUserAvatarColorUseCase(handle)).thenThrow(RuntimeException("boom"))
        whenever(getUserOnlineStatusByHandleUseCase(handle)).thenReturn(UserChatStatus.Offline)

        val result = underTest(chatItem(chatId = 1L, title = "Alice", peerHandle = handle))

        val row = result as ChatExplorerUiItem.OneToOneChat
        assertThat(row.primaryColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `test that contact is mapped to Contact ui item using full name`() = runTest {
        val handle = 300L
        whenever(getUserAvatarColorUseCase(handle)).thenReturn(0xFF7CB342.toInt())
        whenever(getUserOnlineStatusByHandleUseCase(handle)).thenReturn(UserChatStatus.Busy)

        val result =
            underTest(contactItem(handle = handle, email = "brielle@mega.nz", fullName = "Brielle"))

        val row = result as ChatExplorerUiItem.Contact
        assertThat(row.id).isEqualTo(handle)
        assertThat(row.contactName).isEqualTo("Brielle")
        assertThat(row.contactEmail).isEqualTo("brielle@mega.nz")
        assertThat(row.userStatus).isEqualTo(ChatStatus.Busy)
    }

    @Test
    fun `test that contact name falls back to email when full name is missing`() = runTest {
        val handle = 301L
        whenever(getUserAvatarColorUseCase(handle)).thenReturn(0)
        whenever(getUserOnlineStatusByHandleUseCase(handle)).thenReturn(UserChatStatus.Offline)

        val result = underTest(contactItem(handle = handle, email = "anon@mega.nz"))

        val row = result as ChatExplorerUiItem.Contact
        assertThat(row.contactName).isEqualTo("anon@mega.nz")
    }

    @Test
    fun `test that contact returns null when user is missing`() = runTest {
        val result = underTest(UserContact(contact = null, user = null))

        assertThat(result).isNull()
    }
}
