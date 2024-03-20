package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateContactAttachmentMessageUseCaseTest {

    private lateinit var underTest: CreateContactAttachmentMessageUseCase
    private val getUserUseCase: GetUserUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()

    @BeforeAll
    internal fun setUp() {
        underTest = CreateContactAttachmentMessageUseCase(
            getUserUseCase,
            getMyUserHandleUseCase,
            areCredentialsVerifiedUseCase
        )
    }

    @BeforeEach
    internal fun reset() {
        reset(getUserUseCase, getMyUserHandleUseCase, areCredentialsVerifiedUseCase)
    }

    @Test
    fun `test that message returns correctly when user is visible and credentials are verified`() =
        runTest {
            val user = mock<User> {
                on { visibility }.thenReturn(UserVisibility.Visible)
                on { email }.thenReturn("test@test.com")
            }
            val message = mock<ChatMessage> {
                on { userHandles }.thenReturn(listOf(1))
                on { status }.thenReturn(ChatMessageStatus.UNKNOWN)
            }
            whenever(getUserUseCase.invoke(UserId(1))).thenReturn(user)
            whenever(getMyUserHandleUseCase.invoke()).thenReturn(1)
            whenever(areCredentialsVerifiedUseCase.invoke("test@test.com")).thenReturn(true)

            val result = underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    shouldShowAvatar = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )

            assertThat(result).isInstanceOf(ContactAttachmentMessage::class.java)
            assertThat(result.isVerified).isTrue()
            assertThat(result.isContact).isTrue()
        }

    @Test
    fun `test that message returns correctly when user is not visible`() = runTest {
        val user = mock<User> {
            on { visibility }.thenReturn(UserVisibility.Hidden)
        }
        whenever(getUserUseCase.invoke(UserId(1))).thenReturn(user)
        val message = mock<ChatMessage> {
            on { userHandles }.thenReturn(listOf(1))
            on { status }.thenReturn(ChatMessageStatus.UNKNOWN)
        }
        whenever(getMyUserHandleUseCase.invoke()).thenReturn(1)

        val result = underTest.invoke(
            CreateTypedMessageRequest(
                chatMessage = message,
                chatId = 123L,
                isMine = true,
                shouldShowAvatar = true,
                reactions = emptyList(),
                exists = true,
            )
        )

        assertThat(result).isInstanceOf(ContactAttachmentMessage::class.java)
        assertThat(result.isContact).isFalse()
        assertThat(result.isVerified).isFalse()
    }

    @Test
    fun `test that ContactAttachmentMessage is created when user credentials are not verified`() =
        runTest {
            val user = mock<User> {
                on { visibility }.thenReturn(UserVisibility.Visible)
                on { email }.thenReturn("test@test.com")
            }
            val message = mock<ChatMessage> {
                on { userHandles }.thenReturn(listOf(1))
                on { status }.thenReturn(ChatMessageStatus.UNKNOWN)
            }
            whenever(getUserUseCase.invoke(UserId(1))).thenReturn(user)
            whenever(getMyUserHandleUseCase.invoke()).thenReturn(1)
            whenever(areCredentialsVerifiedUseCase.invoke("test@test.com")).thenReturn(false)

            val result = underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    shouldShowAvatar = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )

            assertThat(result).isInstanceOf(ContactAttachmentMessage::class.java)
            assertThat(result.isVerified).isFalse()
            assertThat(result.isContact).isTrue()
        }
}