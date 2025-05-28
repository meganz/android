package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.chat.ChatUploadNotRetriedException
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryChatUploadUseCaseTest {

    private lateinit var underTest: RetryChatUploadUseCase

    private val getPendingMessageUseCase = mock<GetPendingMessageUseCase>()
    private val startChatUploadsWithWorkerUseCase = mock<StartChatUploadsWithWorkerUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase =
        mock<GetOrCreateMyChatsFilesFolderIdUseCase>()

    private val pendingMessageId = 1L
    private val id = NodeId(1)
    private val uriPath = UriPath("foo")
    private val chatUploadAppData = listOf(TransferAppData.ChatUpload(pendingMessageId))
    private val pendingMessage = mock<PendingMessage> {
        on { this.uriPath } doReturn uriPath
    }

    @BeforeAll
    fun setup() {
        underTest = RetryChatUploadUseCase(
            getPendingMessageUseCase = getPendingMessageUseCase,
            startChatUploadsWithWorkerUseCase = startChatUploadsWithWorkerUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase = getOrCreateMyChatsFilesFolderIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPendingMessageUseCase,
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
    fun `test that when GetPendingMessageUseCase returns null, use case does not invoke StartChatUploadsWithWorkerUseCase and throws ChatUploadNotRetriedException`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(null)

            assertThrows<ChatUploadNotRetriedException> { underTest(chatUploadAppData) }

            verifyNoInteractions(startChatUploadsWithWorkerUseCase)
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns a pending message, use case invokes StartChatUploadsWithWorkerUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(pendingMessage)
            whenever(getOrCreateMyChatsFilesFolderIdUseCase()).thenReturn(id)
            whenever(startChatUploadsWithWorkerUseCase(uriPath, id, pendingMessageId))
                .thenReturn(emptyFlow())

            underTest(chatUploadAppData)

            verify(startChatUploadsWithWorkerUseCase).invoke(uriPath, id, pendingMessageId)
        }
}