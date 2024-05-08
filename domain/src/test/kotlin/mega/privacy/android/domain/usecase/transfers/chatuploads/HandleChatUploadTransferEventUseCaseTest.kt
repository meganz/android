package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleChatUploadTransferEventUseCaseTest {

    private lateinit var underTest: HandleChatUploadTransferEventUseCase

    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()
    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()

    @BeforeAll
    fun setup() {
        underTest = HandleChatUploadTransferEventUseCase(
            updatePendingMessageUseCase,
            attachNodeWithPendingMessageUseCase,
        )
    }


    @Test
    fun `test that pending message tag is updated when start event is received`() = runTest {
        val pendingMessageId = 15L
        val transferTag = 12
        val transfer = mock<Transfer> {
            on { it.tag } doReturn transferTag
        }
        val event = MultiTransferEvent.SingleTransferEvent(
            TransferEvent.TransferStartEvent(transfer), 0, 0
        )

        underTest(event, pendingMessageId)

        verify(updatePendingMessageUseCase).invoke(
            UpdatePendingMessageTransferTagRequest(
                pendingMessageId,
                transferTag,
                PendingMessageState.UPLOADING
            )
        )
    }

    @Test
    fun `test that pending message node is attached if already uploaded event is received`() =
        runTest {
            val pendingMessageId = 15L
            val nodeHandle = 12L
            val event = MultiTransferEvent.SingleTransferEvent(
                mock<TransferEvent.TransferFinishEvent>(),
                1L, 1L,
                alreadyTransferredIds = setOf(NodeId(nodeHandle))
            )

            underTest(event, pendingMessageId)
            verify(attachNodeWithPendingMessageUseCase).invoke(
                pendingMessageId,
                NodeId(nodeHandle)
            )
        }

    @Test
    fun `test that pending message is updated to error uploading when a temporary error is received`() =
        runTest {
            val pendingMessageId = 15L
            val event = MultiTransferEvent.SingleTransferEvent(
                mock<TransferEvent.TransferTemporaryErrorEvent>(),
                1L, 1L,
            )

            underTest(event, pendingMessageId)
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_UPLOADING
                )
            )
        }
}