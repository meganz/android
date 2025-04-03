package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleChatTransferEventsUseCaseTest {

    private lateinit var underTest: HandleChatTransferEventsUseCase

    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = HandleChatTransferEventsUseCase(
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
        )
    }

    @Test
    fun `test that node is attached when a finish transfer event with pending message id is received`() =
        runTest {
            val pendingMessageId = 123L
            val nodeHandle = 456L
            val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.nodeHandle } doReturn nodeHandle
                on { this.appData } doReturn appData
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verify(attachNodeWithPendingMessageUseCase).invoke(
                pendingMessageId,
                NodeId(nodeHandle),
                appData
            )
            verifyNoInteractions(updatePendingMessageUseCase)
        }

    @Test
    fun `test that not chat transfer events are filtered out`() =
        runTest {
            val pendingMessageId = 123L
            val nodeHandle = 456L
            val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.nodeHandle } doReturn nodeHandle
                on { this.appData } doReturn appData
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verifyNoInteractions(attachNodeWithPendingMessageUseCase)
            verifyNoInteractions(updatePendingMessageUseCase)
        }

    @Test
    fun `test that not transfer finish events are filtered out`() =
        runTest {
            val pendingMessageId = 123L
            val nodeHandle = 456L
            val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.nodeHandle } doReturn nodeHandle
                on { this.appData } doReturn appData
            }
            val finishEvent = TransferEvent.TransferUpdateEvent(transfer)

            underTest(finishEvent)

            verifyNoInteractions(attachNodeWithPendingMessageUseCase)
            verifyNoInteractions(updatePendingMessageUseCase)
        }

    @Test
    fun `test that pending message is updated to error attaching when an exception occurs while attaching the node`() =
        runTest {
            val pendingMessageId = 123L
            val nodeHandle = 456L
            val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.nodeHandle } doReturn nodeHandle
                on { this.appData } doReturn appData
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)
            whenever(
                attachNodeWithPendingMessageUseCase.invoke(
                    pendingMessageId,
                    NodeId(nodeHandle),
                    appData
                )
            ).thenThrow(RuntimeException())

            underTest(finishEvent)

            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_ATTACHING
                )
            )
        }

    @Test
    fun `test that pending message is updated to error uploading when finish event has an error`() =
        runTest {
            val pendingMessageId = 123L
            val nodeHandle = 456L
            val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.nodeHandle } doReturn nodeHandle
                on { this.appData } doReturn appData
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, mock())

            underTest(finishEvent)

            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_UPLOADING
                )
            )

            verifyNoInteractions(attachNodeWithPendingMessageUseCase)
        }
}