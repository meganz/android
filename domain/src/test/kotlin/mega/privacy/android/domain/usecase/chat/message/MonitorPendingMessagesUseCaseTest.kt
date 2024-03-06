package mega.privacy.android.domain.usecase.chat.message

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPendingMessagesUseCaseTest {
    private lateinit var underTest: MonitorPendingMessagesUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val createPendingAttachmentMessageUseCase =
        mock<CreatePendingAttachmentMessageUseCase>()


    @BeforeEach
    internal fun setUp() {
        underTest = MonitorPendingMessagesUseCase(
            chatMessageRepository,
            createPendingAttachmentMessageUseCase,
        )
    }

    @AfterEach
    internal fun resetMocks() {
        reset(
            chatMessageRepository,
            createPendingAttachmentMessageUseCase,
        )
    }

    @Test
    fun `test that mapped flow from chat message repository is returned`() = runTest {
        val chatId = 45L
        val pendingMessagesMap = (0..10).associate {
            val pendingAttachmentMessage = mock<PendingFileAttachmentMessage>()
            val pendingMessage = mock<PendingMessage>()
            whenever(createPendingAttachmentMessageUseCase(pendingMessage))
                .thenReturn(pendingAttachmentMessage)
            pendingMessage to pendingAttachmentMessage
        }
        whenever(chatMessageRepository.monitorPendingMessages(chatId))
            .thenReturn(flowOf(pendingMessagesMap.keys.toList()))
        val expected = pendingMessagesMap.values.toList()

        underTest(chatId).test {
            val actual = awaitItem()
            assertThat(actual).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }
}