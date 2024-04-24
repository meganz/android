package mega.privacy.android.domain.usecase.chat.message

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetFileForChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendChatAttachmentsUseCaseTest {
    private lateinit var underTest: SendChatAttachmentsUseCase

    private val startChatUploadsWithWorkerUseCase = mock<StartChatUploadsWithWorkerUseCase>()
    private val getFileForChatUploadUseCase = mock<GetFileForChatUploadUseCase>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val deviceCurrentTimeUseCase = mock<GetDeviceCurrentTimeUseCase>()
    private val getMyChatsFilesFolderIdUseCase = mock<GetMyChatsFilesFolderIdUseCase>()

    private val file = mockFile()


    @BeforeAll
    fun setup() {
        underTest = SendChatAttachmentsUseCase(
            startChatUploadsWithWorkerUseCase,
            getFileForChatUploadUseCase,
            chatMessageRepository,
            deviceCurrentTimeUseCase,
            getMyChatsFilesFolderIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            startChatUploadsWithWorkerUseCase,
            getFileForChatUploadUseCase,
            chatMessageRepository,
            deviceCurrentTimeUseCase,
            getMyChatsFilesFolderIdUseCase,
        )
    }

    @Test
    fun `test that correct file is uploaded`() = runTest {
        val chatId = 123L
        val uris = mapOf<String, String?>("file" to null)
        commonStub()
        underTest(uris, false, chatId).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(startChatUploadsWithWorkerUseCase)(eq(file), NodeId(any()), any())
    }

    @Test
    fun `test that each file is uploaded with chat upload`() = runTest {
        val chatId = 123L
        val uris = List(3) { "file$it" }.associateWith { null }
        commonStub()
        uris.keys.forEach {
            val file = mockFile()
            whenever(getFileForChatUploadUseCase(it)).thenReturn(file)
        }
        underTest(uris, false, chatId).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(startChatUploadsWithWorkerUseCase, times(uris.size))(any(), NodeId(any()), any())
    }

    @Test
    fun `test that pending message is saved`() = runTest {
        val chatId = 123L
        val pendingMsgId = 123L
        val uris = mapOf<String, String?>("file" to null)
        commonStub(pendingMsgId)
        underTest(uris, false, chatId).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(startChatUploadsWithWorkerUseCase)(eq(file), NodeId(any()), eq(pendingMsgId))
    }

    @Test
    fun `test that pending message is saved with voice clip type when it's a voice clip`() =
        runTest {
            val chatId = 123L
            val pendingMsgId = 123L
            val uris = mapOf<String, String?>("file" to null)
            commonStub(pendingMsgId)
            underTest(uris, isVoiceClip = true, chatId).test {
                cancelAndIgnoreRemainingEvents()
            }
            verify(chatMessageRepository).savePendingMessages(argThat {
                this.type == PendingMessage.TYPE_VOICE_CLIP
            }, eq(listOf(chatId)))
        }

    @Test
    fun `test that pending message is saved with correct name`() =
        runTest {
            val chatId = 123L
            val pendingMsgId = 123L
            val name = "file renamed"
            val uris = mapOf<String, String?>("file" to name)
            commonStub(pendingMsgId)
            underTest(uris, isVoiceClip = true, chatId).test {
                cancelAndIgnoreRemainingEvents()
            }
            verify(chatMessageRepository).savePendingMessages(argThat {
                this.name == name
            }, eq(listOf(chatId)))
        }

    @Test
    fun `test that getMyChatsFilesFolderIdUseCase is called only once for multiple files`() =
        runTest {
            val chatId = 123L
            val myChatFolderId = NodeId(154L)
            val uris = List(3) { "file$it" }.associateWith { null }
            commonStub(myChatFolderId = myChatFolderId)
            uris.keys.forEach {
                val file = mockFile()
                whenever(getFileForChatUploadUseCase(it)).thenReturn(file)
            }
            underTest(uris, false, chatId).test {
                cancelAndConsumeRemainingEvents()

                verify(startChatUploadsWithWorkerUseCase, times(uris.size))
                    .invoke(any(), NodeId(eq(myChatFolderId.longValue)), any())
                verify(getMyChatsFilesFolderIdUseCase).invoke()
            }
        }

    private suspend fun commonStub(pendingMsgId: Long = 1L, myChatFolderId: NodeId = NodeId(-1)) {
        whenever(chatMessageRepository.savePendingMessages(any(), any()))
            .thenReturn(listOf(pendingMsgId))
        val event = mock<MultiTransferEvent.SingleTransferEvent> {
            on { scanningFinished } doReturn true
        }
        whenever(startChatUploadsWithWorkerUseCase(any(), NodeId(any()), any())).thenReturn(
            flowOf(event)
        )
        whenever(getFileForChatUploadUseCase(any())).thenReturn(file)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(myChatFolderId)
    }

    private fun mockFile() = mock<File> {
        on { path } doReturn "path"
    }
}