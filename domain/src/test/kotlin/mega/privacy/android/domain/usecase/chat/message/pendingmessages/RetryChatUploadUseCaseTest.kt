package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
import mega.privacy.android.domain.usecase.chat.message.CreatePendingAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryChatUploadUseCaseTest {

    private lateinit var underTest: RetryChatUploadUseCase

    private val getPendingMessageUseCase = mock<GetPendingMessageUseCase>()
    private val createPendingAttachmentMessageUseCase =
        mock<CreatePendingAttachmentMessageUseCase>()
    private val startChatUploadsWithWorkerUseCase = mock<StartChatUploadsWithWorkerUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase =
        mock<GetOrCreateMyChatsFilesFolderIdUseCase>()

    private val pendingMessageId = 1L
    private val id = NodeId(1)
    private val file = mock<File>()
    private val chatUploadAppData = listOf(TransferAppData.ChatUpload(pendingMessageId))
    private val pendingMessage = mock<PendingMessage>()
    private val pendingAttachmentMessage = mock<PendingFileAttachmentMessage> {
        on { this.file } doReturn file
    }

    @BeforeAll
    fun setup() {
        underTest = RetryChatUploadUseCase(
            getPendingMessageUseCase = getPendingMessageUseCase,
            createPendingAttachmentMessageUseCase = createPendingAttachmentMessageUseCase,
            startChatUploadsWithWorkerUseCase = startChatUploadsWithWorkerUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase = getOrCreateMyChatsFilesFolderIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPendingMessageUseCase,
            createPendingAttachmentMessageUseCase,
            startChatUploadsWithWorkerUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
        )
    }

    @Test
    fun `test that when chatUploadAppData is empty, use case throws ChatUploadNotRetriedException`() =
        runTest {
            assertThrows<ChatUploadNotRetriedException> { underTest(emptyList()) }
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns null, use case does not invoke CreatePendingAttachmentMessageUseCase and throws ChatUploadNotRetriedException`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(null)

            assertThrows<ChatUploadNotRetriedException> { underTest(chatUploadAppData) }

            verifyNoInteractions(createPendingAttachmentMessageUseCase)
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns a pending message, use case invokes StartChatUploadsWithWorkerUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(pendingMessage)
            whenever(createPendingAttachmentMessageUseCase(pendingMessage))
                .thenReturn(pendingAttachmentMessage)
            whenever(getOrCreateMyChatsFilesFolderIdUseCase()).thenReturn(id)
            whenever(startChatUploadsWithWorkerUseCase(file, id, pendingMessageId))
                .thenReturn(emptyFlow())

            underTest(chatUploadAppData)

            verify(startChatUploadsWithWorkerUseCase).invoke(file, id, pendingMessageId)
        }
}