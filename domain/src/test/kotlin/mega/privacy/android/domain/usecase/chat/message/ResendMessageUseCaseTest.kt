package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.UserMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.retry.RetryMessageUseCase
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
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResendMessageUseCaseTest {
    private lateinit var underTest: ResendMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    private val retryMessageUseCase1 = mock<RetryMessageUseCase>()
    private val retryMessageUseCase2 = mock<RetryMessageUseCase>()
    private val retryMessageUseCase3 = mock<RetryMessageUseCase>()

    private val retryMessageUseCases = setOf<@JvmSuppressWildcards RetryMessageUseCase>(
        retryMessageUseCase1,
        retryMessageUseCase2,
        retryMessageUseCase3,
    )

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
            *retryMessageUseCases.toTypedArray(),
            *forwardMessageUseCases.toTypedArray(),
        )

        underTest = ResendMessageUseCase(
            chatMessageRepository = chatMessageRepository,
            retryMessageUseCases = retryMessageUseCases,
            forwardMessageUseCases = forwardMessageUseCases,
        )
    }

    @Test
    internal fun `test that non user message throws an exception`() = runTest {
        val message = mock<ManagementMessage>()
        assertThrows<IllegalArgumentException> { underTest(message) }
    }

    @Test
    internal fun `test that retry use cases handles the message when corresponds`() = runTest {
        val message = mock<UserMessage>()
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn true
        underTest(message)
        verify(retryMessageUseCase1).invoke(message)
    }

    @Test
    internal fun `test that retry use cases are called until one can handle it`() = runTest {
        val message = mock<UserMessage>()
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn true
        underTest(message)
        verify(retryMessageUseCase1).canRetryMessage(message)
        verify(retryMessageUseCase2).canRetryMessage(message)
        verify(retryMessageUseCase3).invoke(message)
    }

    @Test
    internal fun `test that forward use case is called when no retry message use case handles it`() =
        runTest {
            val expectedChatId = 123L
            val message = mock<UserMessage> {
                on { chatId } doReturn expectedChatId
            }
            whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
            whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
            whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn false
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
    internal fun `test that forward use cases are called until one returns a result when no retry message use case handles it`() =
        runTest {
            val expectedChatId = 123L
            val message = mock<UserMessage> {
                on { chatId } doReturn expectedChatId
            }
            whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
            whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
            whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn false
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
    internal fun `test that message is removed when it is forwarded`() = runTest {
        val message = mock<UserMessage>()
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn false
        forwardMessageUseCase1.stub {
            onBlocking { invoke(any(), any()) } doReturn listOf(ForwardResult.Success(123L))
        }

        underTest(message)

        verify(chatMessageRepository).removeSentMessage(message)
    }


    @Test
    internal fun `test that messages is not removed when it is retried`() = runTest {
        forwardMessageUseCase1.stub {
            onBlocking { invoke(any(), any()) } doReturn listOf(ForwardResult.Success(123L))
        }
        val message = mock<PendingFileAttachmentMessage>()
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn true

        underTest(message)

        verify(chatMessageRepository, never()).removeSentMessage(message)
    }

    @Test
    internal fun `test that an exception is thrown if no use case handles the message`() = runTest {
        val expectedChatId = 123L
        val message = mock<UserMessage> {
            on { chatId } doReturn expectedChatId
        }
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn false
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
        whenever(retryMessageUseCase1.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase2.canRetryMessage(message)) doReturn false
        whenever(retryMessageUseCase3.canRetryMessage(message)) doReturn false
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