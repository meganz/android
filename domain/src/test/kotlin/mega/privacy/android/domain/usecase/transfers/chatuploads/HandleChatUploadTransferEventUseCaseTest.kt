package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
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
    fun `test that pending message tag and uniqueId are updated when start event is received`() =
        runTest {
            val pendingMessageId = 15L
            val uniqueId = 3438L
            val transfer = mock<Transfer> {
                on { it.uniqueId } doReturn uniqueId
            }
            val event = TransferEvent.TransferStartEvent(transfer)

            underTest(event, pendingMessageId)

            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageTransferTagRequest(
                    pendingMessageId,
                    uniqueId,
                    PendingMessageState.UPLOADING
                )
            )
        }

    @Test
    fun `test that pending message node is attached if already uploaded event is received`() =
        runTest {
            val pendingMessageId = 15L
            val nodeHandle = 12L
            val appData = listOf(TransferAppData.Geolocation(345.4, 45.34))
            val transfer = mock<Transfer> {
                on { it.isFinished } doReturn true
                on { it.appData } doReturn appData
                on { it.isAlreadyTransferred } doReturn true
                on { it.nodeHandle } doReturn nodeHandle
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            val event = transferEvent

            underTest(event, pendingMessageId)
            verify(attachNodeWithPendingMessageUseCase).invoke(
                pendingMessageId,
                NodeId(nodeHandle),
                appData,
            )
        }

    @Test
    fun `test that pending message is updated to error uploading when a temporary error is received`() =
        runTest {
            val pendingMessageId = 15L
            val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { transfer } doReturn folderTransfer //to avoid checking isAlreadyTransferredEvent
            }

            underTest(event, pendingMessageId)
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_UPLOADING
                )
            )
        }

    private val folderTransfer = mock<Transfer> {
        on { isFolderTransfer } doReturn true
    }
}