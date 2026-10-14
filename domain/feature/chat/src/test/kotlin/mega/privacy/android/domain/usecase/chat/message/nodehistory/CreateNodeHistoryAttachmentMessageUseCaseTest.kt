package mega.privacy.android.domain.usecase.chat.message.nodehistory

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.message.CreateNodeAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateNodeHistoryAttachmentMessageUseCaseTest {
    private lateinit var underTest: CreateNodeHistoryAttachmentMessageUseCase

    private val createNodeAttachmentMessageUseCase = mock<CreateNodeAttachmentMessageUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()

    private val chatId = 123L
    private val myUserHandle = 456L

    @BeforeAll
    fun setUp() {
        underTest = CreateNodeHistoryAttachmentMessageUseCase(
            createNodeAttachmentMessageUseCase = createNodeAttachmentMessageUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(createNodeAttachmentMessageUseCase, getMyUserHandleUseCase)
        whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)
    }

    @ParameterizedTest(name = "when isMine is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke returns the node attachment message created from the message`(
        isMine: Boolean,
    ) = runTest {
        val message = mock<ChatMessage> {
            on { userHandle } doReturn if (isMine) myUserHandle else 789L
        }
        val expected = mock<NodeAttachmentMessage>()
        val request = CreateTypedMessageRequest(
            chatMessage = message,
            chatId = chatId,
            isMine = isMine,
            reactions = emptyList(),
            exists = true,
        )
        whenever(createNodeAttachmentMessageUseCase(request)).thenReturn(expected)

        val actual = underTest(chatId, message)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns null when the message has no file`() = runTest {
        whenever(createNodeAttachmentMessageUseCase(any())).thenReturn(mock<InvalidMessage>())

        val actual = underTest(chatId, mock())

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke returns null when the message cannot be created`() = runTest {
        whenever(createNodeAttachmentMessageUseCase(any())).thenThrow(RuntimeException())

        val actual = underTest(chatId, mock())

        assertThat(actual).isNull()
    }
}
