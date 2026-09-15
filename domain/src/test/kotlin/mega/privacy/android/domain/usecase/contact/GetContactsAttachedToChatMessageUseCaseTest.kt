package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactsAttachedToChatMessageUseCaseTest {

    private lateinit var underTest: GetContactsAttachedToChatMessageUseCase

    private val chatRepository: ChatRepository = mock()
    private val getUserUseCase: GetUserUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase = mock()

    private val chatId = 123L
    private val messageId = 456L

    @BeforeAll
    internal fun setUp() {
        underTest = GetContactsAttachedToChatMessageUseCase(
            chatRepository = chatRepository,
            getUserUseCase = getUserUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            getUserAvatarColorUseCase = getUserAvatarColorUseCase,
        )
    }

    @BeforeEach
    internal fun reset() {
        reset(
            chatRepository,
            getUserUseCase,
            getMyUserHandleUseCase,
            areCredentialsVerifiedUseCase,
            getUserAvatarColorUseCase,
        )
    }

    @Test
    fun `test that an empty list is returned when the message is null`() = runTest {
        whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(null)

        val result = underTest(chatId, messageId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that every attached user is mapped when the message has multiple contacts`() =
        runTest {
            val message = mock<ChatMessage> {
                on { usersCount }.thenReturn(2L)
                on { userHandles }.thenReturn(listOf(1L, 2L))
                on { userEmails }.thenReturn(listOf("a@test.com", "b@test.com"))
                on { userNames }.thenReturn(listOf("Alice", "Bob"))
            }
            whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
            whenever(getMyUserHandleUseCase()).thenReturn(99L)
            whenever(getUserUseCase(UserId(1L))).thenReturn(null)
            whenever(getUserUseCase(UserId(2L))).thenReturn(null)
            whenever(getUserAvatarColorUseCase(1L)).thenReturn(0xFF111111.toInt())
            whenever(getUserAvatarColorUseCase(2L)).thenReturn(0xFF222222.toInt())

            val result = underTest(chatId, messageId)

            assertThat(result).hasSize(2)
            assertThat(result.map { it.email })
                .containsExactly("a@test.com", "b@test.com").inOrder()
            assertThat(result.map { it.name }).containsExactly("Alice", "Bob").inOrder()
            assertThat(result.map { it.avatarColor })
                .containsExactly(0xFF111111.toInt(), 0xFF222222.toInt()).inOrder()
        }

    @Test
    fun `test that avatarColor is resolved from the avatar color use case`() = runTest {
        val message = mock<ChatMessage> {
            on { usersCount }.thenReturn(1L)
            on { userHandles }.thenReturn(listOf(7L))
            on { userEmails }.thenReturn(listOf("a@test.com"))
            on { userNames }.thenReturn(listOf("Alice"))
        }
        whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
        whenever(getMyUserHandleUseCase()).thenReturn(99L)
        whenever(getUserUseCase(UserId(7L))).thenReturn(null)
        whenever(getUserAvatarColorUseCase(7L)).thenReturn(0xFFABCDEF.toInt())

        val result = underTest(chatId, messageId).single()

        assertThat(result.avatarColor).isEqualTo(0xFFABCDEF.toInt())
    }

    @Test
    fun `test that isMe is true when the attached user matches my handle`() = runTest {
        val message = mock<ChatMessage> {
            on { usersCount }.thenReturn(1L)
            on { userHandles }.thenReturn(listOf(99L))
            on { userEmails }.thenReturn(listOf("me@test.com"))
            on { userNames }.thenReturn(listOf("Me"))
        }
        whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
        whenever(getMyUserHandleUseCase()).thenReturn(99L)
        whenever(getUserUseCase(UserId(99L))).thenReturn(null)
        whenever(getUserAvatarColorUseCase(99L)).thenReturn(0xFF000000.toInt())

        val result = underTest(chatId, messageId)

        assertThat(result.single().isMe).isTrue()
    }

    @Test
    fun `test that isContact and isVerified are true when the user is visible and verified`() =
        runTest {
            val user = mock<User> {
                on { visibility }.thenReturn(UserVisibility.Visible)
            }
            val message = mock<ChatMessage> {
                on { usersCount }.thenReturn(1L)
                on { userHandles }.thenReturn(listOf(1L))
                on { userEmails }.thenReturn(listOf("a@test.com"))
                on { userNames }.thenReturn(listOf("Alice"))
            }
            whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
            whenever(getMyUserHandleUseCase()).thenReturn(99L)
            whenever(getUserUseCase(UserId(1L))).thenReturn(user)
            whenever(areCredentialsVerifiedUseCase("a@test.com")).thenReturn(true)
            whenever(getUserAvatarColorUseCase(1L)).thenReturn(0xFF000000.toInt())

            val result = underTest(chatId, messageId).single()

            assertThat(result.isContact).isTrue()
            assertThat(result.isVerified).isTrue()
        }

    @Test
    fun `test that isContact is true but isVerified is false when credentials are not verified`() =
        runTest {
            val user = mock<User> {
                on { visibility }.thenReturn(UserVisibility.Visible)
            }
            val message = mock<ChatMessage> {
                on { usersCount }.thenReturn(1L)
                on { userHandles }.thenReturn(listOf(1L))
                on { userEmails }.thenReturn(listOf("a@test.com"))
                on { userNames }.thenReturn(listOf("Alice"))
            }
            whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
            whenever(getMyUserHandleUseCase()).thenReturn(99L)
            whenever(getUserUseCase(UserId(1L))).thenReturn(user)
            whenever(areCredentialsVerifiedUseCase("a@test.com")).thenReturn(false)
            whenever(getUserAvatarColorUseCase(1L)).thenReturn(0xFF000000.toInt())

            val result = underTest(chatId, messageId).single()

            assertThat(result.isContact).isTrue()
            assertThat(result.isVerified).isFalse()
        }

    @Test
    fun `test that isContact and isVerified are false when the user is not visible`() = runTest {
        val user = mock<User> {
            on { visibility }.thenReturn(UserVisibility.Hidden)
        }
        val message = mock<ChatMessage> {
            on { usersCount }.thenReturn(1L)
            on { userHandles }.thenReturn(listOf(1L))
            on { userEmails }.thenReturn(listOf("a@test.com"))
            on { userNames }.thenReturn(listOf("Alice"))
        }
        whenever(chatRepository.getMessage(chatId, messageId)).thenReturn(message)
        whenever(getMyUserHandleUseCase()).thenReturn(99L)
        whenever(getUserUseCase(UserId(1L))).thenReturn(user)
        whenever(getUserAvatarColorUseCase(1L)).thenReturn(0xFF000000.toInt())

        val result = underTest(chatId, messageId).single()

        assertThat(result.isContact).isFalse()
        assertThat(result.isVerified).isFalse()
    }
}
