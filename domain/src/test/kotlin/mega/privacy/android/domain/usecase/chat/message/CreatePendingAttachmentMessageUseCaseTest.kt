package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreatePendingAttachmentMessageUseCaseTest {
    private lateinit var underTest: CreatePendingAttachmentMessageUseCase

    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = CreatePendingAttachmentMessageUseCase(
            getMyUserHandleUseCase,
            fileSystemRepository,
        )
    }

    @AfterEach
    internal fun resetMocks() {
        reset(
            getMyUserHandleUseCase,
            fileSystemRepository,
        )
    }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that PendingFileAttachmentMessage with correct values is returned when pending message is not a voice clip`(
        state: PendingMessageState,
    ) = runTest {
        val chatId = 156L
        val msgId = 87L
        val time = 72834578L
        val userHandle = 245L
        val filePath = "filepath"
        val fileName = "fileName"
        val transferTag = 344
        val fileTypeInfo = mock<UnknownFileTypeInfo>()
        val fileSize = 89475L
        whenever(getMyUserHandleUseCase()).thenReturn(userHandle)
        whenever(fileSystemRepository.getFileTypeInfo(File(filePath))) doReturn fileTypeInfo
        whenever(fileSystemRepository.getTotalSize(File(filePath))) doReturn fileSize
        val pendingMessage = PendingMessage(
            id = msgId,
            chatId = chatId,
            uploadTimestamp = time,
            state = state.value,
            filePath = filePath,
            transferTag = transferTag,
            name = fileName,
        )
        val expected = PendingFileAttachmentMessage(
            chatId = chatId,
            msgId = msgId,
            time = time,
            isDeletable = true,
            isEditable = false,
            userHandle = userHandle,
            shouldShowAvatar = false,
            reactions = emptyList(),
            status = getChatMessageStatus(state),
            content = null,
            filePath = filePath,
            fileType = fileTypeInfo,
            transferTag = transferTag,
            nodeId = null,
            state = state,
            fileName = fileName,
            fileSize = fileSize,
        )
        val actual = underTest(pendingMessage)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that PendingVoiceClipMessage with correct values is returned when pending message is a voice clip`(
        state: PendingMessageState,
    ) = runTest {
        val chatId = 156L
        val msgId = 87L
        val time = 72834578L
        val userHandle = 245L
        val filePath = "filepath"
        val transferTag = 344
        val fileTypeInfo = mock<UnknownFileTypeInfo>()
        val fileName = "fileName"
        whenever(getMyUserHandleUseCase()).thenReturn(userHandle)
        whenever(fileSystemRepository.getFileTypeInfo(File(filePath)))
            .thenReturn(fileTypeInfo)
        val pendingMessage = PendingMessage(
            id = msgId,
            chatId = chatId,
            uploadTimestamp = time,
            state = state.value,
            filePath = filePath,
            type = PendingMessage.TYPE_VOICE_CLIP,
            transferTag = transferTag,
            name = fileName,
        )
        val expected = PendingVoiceClipMessage(
            chatId = chatId,
            msgId = msgId,
            time = time,
            isDeletable = true,
            isEditable = false,
            userHandle = userHandle,
            shouldShowAvatar = false,
            reactions = emptyList(),
            status = getChatMessageStatus(state),
            content = null,
            fileType = fileTypeInfo,
            transferTag = transferTag,
            nodeId = null,
            state = state,
            filePath = filePath,
            fileName = fileName,
        )
        val actual = underTest(pendingMessage)
        assertThat(actual).isEqualTo(expected)
    }

    private fun getChatMessageStatus(state: PendingMessageState) =
        when (state) {
            PendingMessageState.ATTACHING -> ChatMessageStatus.SENDING
            PendingMessageState.SENT -> ChatMessageStatus.DELIVERED
            PendingMessageState.ERROR_ATTACHING -> ChatMessageStatus.SERVER_REJECTED
            PendingMessageState.ERROR_UPLOADING -> ChatMessageStatus.SENDING_MANUAL
            else -> ChatMessageStatus.UNKNOWN
        }
}
