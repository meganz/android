package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.file.DoesCacheHaveSufficientSpaceForUrisUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendChatAttachmentsUseCaseTest {
    private lateinit var underTest: SendChatAttachmentsUseCase

    private val startChatUploadsWorkerUseCase = mock<StartChatUploadsWorkerUseCase>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val deviceCurrentTimeUseCase = mock<GetDeviceCurrentTimeUseCase>()
    private val doesCacheHaveSufficientSpaceForUrisUseCase =
        mock<DoesCacheHaveSufficientSpaceForUrisUseCase>()

    @BeforeAll
    fun setup() {
        underTest = SendChatAttachmentsUseCase(
            startChatUploadsWorkerUseCase,
            chatMessageRepository,
            deviceCurrentTimeUseCase,
            doesCacheHaveSufficientSpaceForUrisUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            startChatUploadsWorkerUseCase,
            chatMessageRepository,
            deviceCurrentTimeUseCase,
            doesCacheHaveSufficientSpaceForUrisUseCase,
        )
    }

    @Test
    fun `test that correct pending message is created`() = runTest {
        val chatId = 123L
        val uri = "file"
        val uris = mapOf<String, String?>(uri to null)
        commonStub()
        underTest(uris, false, chatId)
        verify(chatMessageRepository).savePendingMessages(
            SavePendingMessageRequest(
                chatId = chatId,
                type = -1,
                uploadTimestamp = deviceCurrentTimeUseCase() / 1000,
                state = PendingMessageState.PREPARING,
                tempIdKarere = -1,
                videoDownSampled = null,
                filePath = uri,
                nodeHandle = -1,
                fingerprint = null,
                name = uris[uri],
                transferTag = -1,
            ),
            listOf(chatId)
        )
    }

    @Test
    fun `test that a pending message is created for each file`() = runTest {
        val chatId = 123L
        val uris = List(3) { "file$it" }.associateWith { null }
        commonStub()
        underTest(uris, false, chatId)
        verify(chatMessageRepository, times(uris.size))
            .savePendingMessages(any(), eq(listOf(chatId)))
    }

    @Test
    fun `test that pending message is saved with voice clip type when it's a voice clip`() =
        runTest {
            val chatId = 123L
            val pendingMsgId = 123L
            val uris = mapOf<String, String?>("file" to null)
            commonStub(pendingMsgId)
            underTest(uris, isVoiceClip = true, chatId)
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
            underTest(uris, isVoiceClip = true, chatId)
            verify(chatMessageRepository).savePendingMessages(argThat {
                this.name == name
            }, eq(listOf(chatId)))
        }

    @Test
    fun `test that original path is cached`() = runTest {
        val chatId = 1263L
        val pendingMsgId = 22353L
        val expected = "content://content.example"
        val uris = mapOf<String, String?>(expected to null)
        commonStub(pendingMsgId)

        underTest(uris, isVoiceClip = false, chatId)

        verify(chatMessageRepository).cacheOriginalPathForPendingMessage(
            pendingMsgId, expected
        )
    }

    private suspend fun commonStub(pendingMsgId: Long = 1L) {
        whenever(chatMessageRepository.savePendingMessages(any(), any()))
            .thenReturn(listOf(pendingMsgId))
        whenever(doesCacheHaveSufficientSpaceForUrisUseCase(any()))
            .thenReturn(true)
    }
}