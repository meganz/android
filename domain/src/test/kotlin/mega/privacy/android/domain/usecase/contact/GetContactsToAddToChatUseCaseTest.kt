package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactsToAddToChatUseCaseTest {

    private lateinit var underTest: GetContactsToAddToChatUseCase

    private val getContactsUseCase = mock<GetContactsUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    private val chatId = 123L

    @BeforeEach
    fun setUp() {
        reset(getContactsUseCase, getChatRoomUseCase)
        underTest = GetContactsToAddToChatUseCase(
            getContactsUseCase = getContactsUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
        )
    }

    @Test
    fun `test that contacts already in the chat are excluded`() = runTest {
        val alice = contactItem(handle = 1L, email = "alice@example.com")
        val bob = contactItem(handle = 2L, email = "bob@example.com")
        whenever(getContactsUseCase()).thenReturn(flowOf(listOf(alice, bob)))
        stubChatRoom(peerHandles = listOf(2L))

        underTest(chatId).test {
            assertThat(awaitItem()).containsExactly(alice)
            awaitComplete()
        }
    }

    @Test
    fun `test that all contacts are returned when the chat has no participants`() = runTest {
        val alice = contactItem(handle = 1L, email = "alice@example.com")
        val bob = contactItem(handle = 2L, email = "bob@example.com")
        whenever(getContactsUseCase()).thenReturn(flowOf(listOf(alice, bob)))
        stubChatRoom(peerHandles = emptyList())

        underTest(chatId).test {
            assertThat(awaitItem()).containsExactly(alice, bob)
            awaitComplete()
        }
    }

    @Test
    fun `test that all contacts are returned when the chat room is null`() = runTest {
        val alice = contactItem(handle = 1L, email = "alice@example.com")
        whenever(getContactsUseCase()).thenReturn(flowOf(listOf(alice)))
        getChatRoomUseCase.stub { onBlocking { invoke(chatId) }.doReturn(null) }

        underTest(chatId).test {
            assertThat(awaitItem()).containsExactly(alice)
            awaitComplete()
        }
    }

    @Test
    fun `test that the latest chat participants are used on each emission`() = runTest {
        val alice = contactItem(handle = 1L, email = "alice@example.com")
        val bob = contactItem(handle = 2L, email = "bob@example.com")
        whenever(getContactsUseCase())
            .thenReturn(flowOf(listOf(alice, bob), listOf(alice, bob)))
        val chatRoom = mock<ChatRoom> {
            on { peerHandlesList } doReturn emptyList() doReturn listOf(2L)
        }
        getChatRoomUseCase.stub { onBlocking { invoke(chatId) }.doReturn(chatRoom) }

        underTest(chatId).test {
            assertThat(awaitItem()).containsExactly(alice, bob)
            assertThat(awaitItem()).containsExactly(alice)
            awaitComplete()
        }
    }

    private fun stubChatRoom(peerHandles: List<Long>) {
        val chatRoom = mock<ChatRoom> { on { peerHandlesList } doReturn peerHandles }
        getChatRoomUseCase.stub { onBlocking { invoke(chatId) }.doReturn(chatRoom) }
    }

    private fun contactItem(handle: Long, email: String) = ContactItem(
        handle = handle,
        email = email,
        contactData = ContactData(
            fullName = null,
            alias = null,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = null,
        visibility = UserVisibility.Visible,
        lastSeen = null,
        timestamp = 0L,
        status = UserChatStatus.Offline,
        areCredentialsVerified = false,
        chatroomId = null,
    )
}
