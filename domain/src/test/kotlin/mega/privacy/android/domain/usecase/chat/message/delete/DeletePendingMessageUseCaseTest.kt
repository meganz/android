package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeletePendingMessageUseCaseTest {
    private lateinit var underTest: DeletePendingMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeletePendingMessageUseCase(
            chatMessageRepository,
            cancelTransferByTagUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatMessageRepository,
            cancelTransferByTagUseCase,
        )
    }

    @Test
    fun `test that delete pending message invokes the correct method in chat message repository`() =
        runTest {
            val expected = 15L
            underTest.invoke(listOf(mock<PendingFileAttachmentMessage> {
                on { msgId } doReturn expected
            }))
            verify(chatMessageRepository).deletePendingMessageById(expected)
        }

    @Test
    fun `test that cancelTransferByTagUseCase is invoked when this use case is invoked with a pending message with a transfer tag`() =
        runTest {
            val expected = 15
            underTest.invoke(listOf(mock<PendingFileAttachmentMessage> {
                on { transferTag } doReturn expected
            }))
            verify(cancelTransferByTagUseCase).invoke(expected)
        }
}