package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndNodeHandleRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.GetPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
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
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()
    private val getPendingMessageUseCase = mock<GetPendingMessageUseCase>()

    @BeforeAll
    fun setup() {
        underTest = AttachNodeWithPendingMessageUseCase(
            chatMessageRepository = chatMessageRepository,
            chatRepository = chatRepository,
            getChatMessageUseCase = getChatMessageUseCase,
            createSaveSentMessageRequestUseCase = createSaveSentMessageRequestUseCase,
            setNodeAttributesAfterUploadUseCase = setNodeAttributesAfterUploadUseCase,
            updatePendingMessageUseCase = updatePendingMessageUseCase,
            getPendingMessageUseCase = getPendingMessageUseCase,
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
            updatePendingMessageUseCase,
            getPendingMessageUseCase,
        )
    }

    @Test
    fun `test that pending message is updated to attaching when use case is invoked`() = runTest {
        val pendingMessage = createPendingMessageMock()
        val nodeId = 34L
        whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(pendingMessage)

        underTest(pendingMsgId, NodeId(nodeId))
        verify(updatePendingMessageUseCase)
            .invoke(
                UpdatePendingMessageStateAndNodeHandleRequest(
                    pendingMsgId,
                    nodeHandle = nodeId,
                    state = PendingMessageState.ATTACHING
                )
            )
    }

    @Test
    fun `test that pending message is updated to error attaching when attachNode fails`() =
        runTest {
            val pendingMessage = createPendingMessageMock()
            whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.attachNode(chatId, NodeId(nodeHandle))).thenReturn(null)

            underTest(pendingMsgId, NodeId(1L))
            verify(updatePendingMessageUseCase)
                .invoke(
                    UpdatePendingMessageStateRequest(
                        pendingMsgId,
                        state = PendingMessageState.ERROR_ATTACHING
                    )
                )
        }

    @Test
    fun `test that pending message is deleted when attachNode succeeds`() =
        runTest {
            val message = mock<ChatMessage>()
            val pendingMessage = createPendingMessageMock()
            val createTypedMessageRequest = mock<CreateTypedMessageRequest>()

            whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.attachNode(chatId, NodeId(nodeHandle))).thenReturn(msgId)
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
            whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(
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
            inOrder.verify(chatMessageRepository).attachNode(chatId, nodeId)
        }

    @Test
    fun `test that file path is cached when original path from pending message does not exist`() =
        runTest {
            val pendingMessage = createPendingMessageMock()
            val nodeId = NodeId(1L)
            whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.getCachedOriginalPathForPendingMessage(pendingMsgId))
                .thenReturn(null)

            underTest(pendingMsgId, nodeId)
            verify(chatMessageRepository)
                .cacheOriginalPathForNode(nodeId, filePath)
        }

    @Test
    fun `test that original path from pending message is cached when exists`() = runTest {
        val pendingMessage = createPendingMessageMock()
        val nodeId = NodeId(1L)
        val originalUri = "content://example.uri"
        whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(pendingMessage)
        whenever(chatMessageRepository.getCachedOriginalPathForPendingMessage(pendingMsgId))
            .thenReturn(originalUri)

        underTest(pendingMsgId, nodeId)
        verify(chatMessageRepository)
            .cacheOriginalPathForNode(nodeId, originalUri)
    }

    @Test
    fun `test that the node is attached as a voice clip when pending message type is voice clip`() =
        runTest {
            val pendingMessage = createPendingMessageMock()
            whenever(pendingMessage.isVoiceClip) doReturn true
            whenever(getPendingMessageUseCase(pendingMsgId)).thenReturn(
                pendingMessage
            )
            whenever(chatMessageRepository.attachVoiceMessage(chatId, nodeHandle)).thenReturn(null)

            underTest(pendingMsgId, NodeId(nodeHandle))
            verify(chatMessageRepository).attachVoiceMessage(chatId, nodeHandle)
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