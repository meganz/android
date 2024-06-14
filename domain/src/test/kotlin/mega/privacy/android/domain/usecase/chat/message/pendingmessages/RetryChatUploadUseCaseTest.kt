package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.chat.message.CreatePendingAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.ResendMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryChatUploadUseCaseTest {

    private lateinit var underTest: RetryChatUploadUseCase

    private val getPendingMessageUseCase = mock<GetPendingMessageUseCase>()
    private val createPendingAttachmentMessageUseCase =
        mock<CreatePendingAttachmentMessageUseCase>()
    private val resendMessageUseCase = mock<ResendMessageUseCase>()

    private val pendingMessageId = 1L
    private val chatUploadAppData = listOf(TransferAppData.ChatUpload(pendingMessageId))
    private val pendingMessage = mock<PendingMessage>()
    private val pendingAttachmentMessage = mock<PendingFileAttachmentMessage>()

    @BeforeAll
    fun setup() {
        underTest = RetryChatUploadUseCase(
            getPendingMessageUseCase = getPendingMessageUseCase,
            createPendingAttachmentMessageUseCase = createPendingAttachmentMessageUseCase,
            resendMessageUseCase = resendMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPendingMessageUseCase,
            createPendingAttachmentMessageUseCase,
            resendMessageUseCase,
        )
    }

    @Test
    fun `test that when GetPendingMessageUseCase returns null, use case does not invoke CreatePendingAttachmentMessageUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(null)

            underTest(chatUploadAppData)

            verifyNoInteractions(createPendingAttachmentMessageUseCase)
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns null, use case does not invoke ResendMessageUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(null)

            underTest(chatUploadAppData)

            verifyNoInteractions(resendMessageUseCase)
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns a pending message, use case invokes CreatePendingAttachmentMessageUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(pendingMessage)
            whenever(createPendingAttachmentMessageUseCase(pendingMessage))
                .thenReturn(pendingAttachmentMessage)

            underTest(chatUploadAppData)

            verify(createPendingAttachmentMessageUseCase).invoke(pendingMessage)
        }

    @Test
    fun `test that when GetPendingMessageUseCase returns a pending message, use case invokes ResendMessageUseCase`() =
        runTest {
            whenever(getPendingMessageUseCase(pendingMessageId)).thenReturn(pendingMessage)
            whenever(createPendingAttachmentMessageUseCase(pendingMessage))
                .thenReturn(pendingAttachmentMessage)

            underTest(chatUploadAppData)

            verify(resendMessageUseCase).invoke(pendingAttachmentMessage)
        }
}