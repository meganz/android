package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckFinishedChatUploadsUseCaseTest {
    private lateinit var underTest: CheckFinishedChatUploadsUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val transferRepository = mock<TransferRepository>()
    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()

    @BeforeAll
    fun setup() {

        underTest = CheckFinishedChatUploadsUseCase(
            chatMessageRepository,
            transferRepository,
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        chatMessageRepository,
        transferRepository,
        attachNodeWithPendingMessageUseCase,
        updatePendingMessageUseCase,
    )

    @Test
    fun `test that pending messages in uploading state are set to error uploading state when there is no current transfer with its tag`() =
        runTest {
            val pendingMessageIds = (1..10).map { it * 100L }
            val uploadingPendingMessage = pendingMessageIds.mapIndexed { index, id ->
                mock<PendingMessage> {
                    on { it.transferTag } doReturn index
                    on { it.id } doReturn id
                }
            }
            whenever(chatMessageRepository.getPendingMessagesByState(PendingMessageState.UPLOADING)) doReturn uploadingPendingMessage
            whenever(transferRepository.getTransferByTag(any())) doReturn null

            underTest()

            pendingMessageIds.forEach {
                verify(updatePendingMessageUseCase).invoke(
                    UpdatePendingMessageStateRequest(
                        it,
                        PendingMessageState.ERROR_UPLOADING
                    )
                )
            }
        }

    @Test
    fun `test that pending messages in uploading state are attached to chat when there is a transfers and the handle is valid`() =
        runTest {
            val pendingMessageIds = (1..10).map { it * 100L }
            val nodeHandles = pendingMessageIds.map { it * 2 }
            val uploadingPendingMessage = pendingMessageIds.mapIndexed { index, id ->
                mock<PendingMessage> {
                    on { it.transferTag } doReturn index
                    on { it.id } doReturn id
                }
            }
            whenever(chatMessageRepository.getPendingMessagesByState(PendingMessageState.UPLOADING)) doReturn uploadingPendingMessage
            uploadingPendingMessage.indices.forEach { index ->
                val transfer = mock<Transfer> {
                    on { it.nodeHandle } doReturn nodeHandles[index]
                    on { it.isFinished } doReturn true
                }
                whenever(transferRepository.getTransferByTag(index)) doReturn transfer
            }

            underTest()

            pendingMessageIds.forEachIndexed { index, pendingMessageId ->
                verify(attachNodeWithPendingMessageUseCase).invoke(
                    pendingMessageId,
                    NodeId(nodeHandles[index])
                )
            }
        }

    @Test
    fun `test that pending messages in uploading state are set to error uploading state when there is a transfers but with invalid handle`() =
        runTest {
            val pendingMessageIds = (1..10).map { it * 100L }
            val uploadingPendingMessage = pendingMessageIds.mapIndexed { index, id ->
                mock<PendingMessage> {
                    on { it.transferTag } doReturn index
                    on { it.id } doReturn id
                }
            }
            whenever(chatMessageRepository.getPendingMessagesByState(PendingMessageState.UPLOADING)) doReturn uploadingPendingMessage
            uploadingPendingMessage.indices.forEach { index ->
                val transfer = mock<Transfer> {
                    on { it.nodeHandle } doReturn -1
                    on { it.isFinished } doReturn true
                }
                whenever(transferRepository.getTransferByTag(index)) doReturn transfer
            }

            underTest()

            pendingMessageIds.forEach {
                verify(updatePendingMessageUseCase).invoke(
                    UpdatePendingMessageStateRequest(
                        it,
                        PendingMessageState.ERROR_UPLOADING
                    )
                )
            }
        }
}