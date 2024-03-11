package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.UserMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.delete.DeletePendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessageUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResendMessageUseCaseTest {
    private lateinit var underTest: ResendMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val deletePendingMessageUseCase = mock<DeletePendingMessageUseCase>()

    private val forwardMessageUseCase1 = mock<ForwardMessageUseCase>()
    private val forwardMessageUseCase2 = mock<ForwardMessageUseCase>()
    private val forwardMessageUseCase3 = mock<ForwardMessageUseCase>()

    private val forwardMessageUseCases = setOf<@JvmSuppressWildcards ForwardMessageUseCase>(
        forwardMessageUseCase1,
        forwardMessageUseCase2,
        forwardMessageUseCase3,
    )

    @BeforeEach
    internal fun setUp() {
        Mockito.reset(
            chatMessageRepository,
            deletePendingMessageUseCase,
            forwardMessageUseCase1,
        )
        underTest = ResendMessageUseCase(
            chatMessageRepository = chatMessageRepository,
            deletePendingMessageUseCase = deletePendingMessageUseCase,
            forwardMessageUseCases = forwardMessageUseCases,
        )
    }

    @Test
    internal fun `test that non user message throws an exception`() = runTest {
        val message = mock<ManagementMessage>()
        assertThrows<IllegalArgumentException> { underTest(message) }
    }

    @Test
    internal fun `test that forward use case is called`() = runTest {
        val expectedChatId = 123L
        val message = mock<UserMessage> {
            on { chatId } doReturn expectedChatId
        }
        forwardMessageUseCase1.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn listOf(ForwardResult.Success(expectedChatId))
        }
        underTest(message)

        verify(forwardMessageUseCase1).invoke(listOf(expectedChatId), message)
    }

    @Test
    internal fun `test that use cases are called until one returns a result`() = runTest {
        val expectedChatId = 123L
        val message = mock<UserMessage> {
            on { chatId } doReturn expectedChatId
        }
        forwardMessageUseCase1.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn emptyList()
        }

        forwardMessageUseCase2.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn emptyList()
        }

        forwardMessageUseCase3.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn listOf(ForwardResult.Success(expectedChatId))
        }

        underTest(message)

        verify(forwardMessageUseCase1).invoke(listOf(expectedChatId), message)
        verify(forwardMessageUseCase2).invoke(listOf(expectedChatId), message)
        verify(forwardMessageUseCase3).invoke(listOf(expectedChatId), message)
    }


    @Test
    internal fun `test that message is removed`() = runTest {
        forwardMessageUseCase1.stub {
            onBlocking { invoke(any(), any()) } doReturn listOf(ForwardResult.Success(123L))
        }
        val message = mock<UserMessage>()
        underTest(message)

        verify(chatMessageRepository).removeSentMessage(message)
    }


    @Test
    internal fun `test that pending messages are not removed`() = runTest {
        forwardMessageUseCase1.stub {
            onBlocking { invoke(any(), any()) } doReturn listOf(ForwardResult.Success(123L))
        }
        val message = mock<PendingFileAttachmentMessage>()
        underTest(message)

        verify(chatMessageRepository, never()).removeSentMessage(message)
    }

    @Test
    internal fun `test that pending messages are deleted`() = runTest {
        forwardMessageUseCase1.stub {
            onBlocking { invoke(any(), any()) } doReturn listOf(ForwardResult.Success(123L))
        }
        val message = mock<PendingVoiceClipMessage>()
        underTest(message)

        verify(deletePendingMessageUseCase).invoke(listOf(message))
    }

    @Test
    internal fun `test that an exception is thrown if no use case handles the message`() = runTest {
        val expectedChatId = 123L
        val message = mock<UserMessage> {
            on { chatId } doReturn expectedChatId
        }
        forwardMessageUseCase1.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn emptyList()
        }

        forwardMessageUseCase2.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn emptyList()
        }

        forwardMessageUseCase3.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn emptyList()
        }

        assertThrows<IllegalStateException> { underTest(message) }
    }

    @Test
    internal fun `test that exception is thrown if forward use case returns an error`() = runTest {
        val expectedChatId = 123L
        val message = mock<UserMessage> {
            on { chatId } doReturn expectedChatId
        }
        forwardMessageUseCase1.stub {
            onBlocking {
                invoke(
                    listOf(expectedChatId),
                    message
                )
            } doReturn listOf(ForwardResult.ErrorNotAvailable)
        }

        assertThrows<Exception> { underTest(message) }
    }


}