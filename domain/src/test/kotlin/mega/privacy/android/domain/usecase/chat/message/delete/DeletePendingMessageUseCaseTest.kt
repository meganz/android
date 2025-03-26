package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeletePendingMessageUseCaseTest {
    private lateinit var underTest: DeletePendingMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val getTransferByUniqueIdUseCase = mock<GetTransferByUniqueIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeletePendingMessageUseCase(
            chatMessageRepository = chatMessageRepository,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            getTransferByUniqueIdUseCase = getTransferByUniqueIdUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatMessageRepository,
            cancelTransferByTagUseCase,
            getTransferByUniqueIdUseCase,
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
    fun `test that cancelTransferByTagUseCase is invoked when this use case is invoked with a pending message with a valid transfer unique id`() =
        runTest {
            val expected = 15
            val transfer = mock<Transfer> {
                on { tag } doReturn expected
            }

            whenever(getTransferByUniqueIdUseCase(expected.toLong())).thenReturn(transfer)

            underTest.invoke(listOf(mock<PendingFileAttachmentMessage> {
                on { transferUniqueId } doReturn expected.toLong()
            }))

            verify(cancelTransferByTagUseCase).invoke(expected)
        }

    @Test
    fun `test that cancelTransferByTagUseCase is not invoked when this use case is invoked with a pending message with an invalid transfer unique id`() =
        runTest {
            val expected = 15

            whenever(getTransferByUniqueIdUseCase(expected.toLong())).thenReturn(null)

            underTest.invoke(listOf(mock<PendingFileAttachmentMessage> {
                on { transferUniqueId } doReturn expected.toLong()
            }))

            verifyNoInteractions(cancelTransferByTagUseCase)
        }
}