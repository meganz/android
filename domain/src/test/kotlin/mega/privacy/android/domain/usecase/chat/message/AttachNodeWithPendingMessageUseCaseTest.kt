package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachNodeWithPendingMessageUseCaseTest {
    private lateinit var underTest: AttachNodeWithPendingMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()
    private val setNodeAttributesAfterUploadUseCase = mock<SetNodeAttributesAfterUploadUseCase>()

    @BeforeAll
    fun setup() {
        underTest = AttachNodeWithPendingMessageUseCase(
            chatMessageRepository,
            chatRepository,
            getChatMessageUseCase,
            createSaveSentMessageRequestUseCase,
            setNodeAttributesAfterUploadUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            chatMessageRepository,
            chatRepository,
            getChatMessageUseCase,
            createSaveSentMessageRequestUseCase,
            setNodeAttributesAfterUploadUseCase,
        )
    }

    @Test
    fun `test that pending message is updated to attaching when use case is invoked`() = runTest {
        val pendingMessage = createPendingMessageMock()
        whenever(chatMessageRepository.getPendingMessage(pendingMsgId)).thenReturn(pendingMessage)

        underTest(pendingMsgId, NodeId(1L))
        verify(chatMessageRepository)
            .savePendingMessage(
                eq(pendingMessage.createRequest(PendingMessageState.ATTACHING))
            )
    }

    @Test
    fun `test that pending message is updated to error attaching when attachNode fails`() =
        runTest {
            val pendingMessage = createPendingMessageMock()
            whenever(chatMessageRepository.getPendingMessage(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.attachNode(chatId, nodeHandle)).thenReturn(null)

            underTest(pendingMsgId, NodeId(1L))
            verify(chatMessageRepository)
                .savePendingMessage(
                    eq(pendingMessage.createRequest(PendingMessageState.ERROR_ATTACHING))
                )
        }

    @Test
    fun `test that pending message is deleted when attachNode succeeds`() =
        runTest {
            val message = mock<ChatMessage>()
            val pendingMessage = createPendingMessageMock()
            val createTypedMessageRequest = mock<CreateTypedMessageRequest>()

            whenever(chatMessageRepository.getPendingMessage(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.attachNode(chatId, nodeHandle)).thenReturn(msgId)
            whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)
            whenever(createSaveSentMessageRequestUseCase(message, chatId))
                .thenReturn(createTypedMessageRequest)

            underTest(pendingMsgId, NodeId(1L))
            verify(chatRepository).storeMessages(listOf(createTypedMessageRequest))
            verify(chatMessageRepository).deletePendingMessage(pendingMessage)
        }

    @Test
    fun `test that setNodeAttributesAfterUploadUseCase is invoked before attaching the file`() =
        runTest {
            val pendingMessage = createPendingMessageMock()
            whenever(chatMessageRepository.getPendingMessage(pendingMsgId)).thenReturn(
                pendingMessage
            )

            val nodeId = NodeId(nodeHandle)
            val inOrder = inOrder(
                setNodeAttributesAfterUploadUseCase,
                chatMessageRepository
            )

            underTest(pendingMsgId, nodeId)
            inOrder.verify(setNodeAttributesAfterUploadUseCase)
                .invoke(
                    nodeId.longValue,
                    File(filePath),
                )
            inOrder.verify(chatMessageRepository).attachNode(any(), any())
        }

    private fun createPendingMessageMock() = mock<PendingMessage> {
        on { id } doReturn pendingMsgId
        on { chatId } doReturn chatId
        on { type } doReturn type
        on { uploadTimestamp } doReturn timestamp
        on { state } doReturn PendingMessageState.UPLOADING.value
        on { tempIdKarere } doReturn tempId
        on { videoDownSampled } doReturn null
        on { filePath } doReturn filePath
        on { nodeHandle } doReturn nodeHandle
        on { fingerprint } doReturn null
        on { name } doReturn null
        on { transferTag } doReturn transferTag
    }

    private fun PendingMessage.createRequest(state: PendingMessageState) =
        SavePendingMessageRequest(
            chatId = this.chatId,
            type = this.type,
            uploadTimestamp = this.uploadTimestamp,
            state = state,
            tempIdKarere = this.tempIdKarere,
            videoDownSampled = this.videoDownSampled,
            filePath = this.filePath,
            nodeHandle = nodeHandle,
            fingerprint = this.fingerprint,
            name = this.name,
            transferTag = this.transferTag,
        )

    private val pendingMsgId = 43L
    private val chatId = 12L
    private val msgId = 64L
    private val type = -1
    private val timestamp = 1573483L
    private val tempId = 15L
    private val filePath = "file path"
    private val nodeHandle = 1L
    private val transferTag = 78
}