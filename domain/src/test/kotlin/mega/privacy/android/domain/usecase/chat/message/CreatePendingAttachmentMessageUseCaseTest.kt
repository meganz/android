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
        val fileTypeInfo = mock<UnknownFileTypeInfo>()
        whenever(getMyUserHandleUseCase()).thenReturn(userHandle)
        whenever(fileSystemRepository.getFileTypeInfo(File(filePath)))
            .thenReturn(fileTypeInfo)
        val pendingMessage = PendingMessage(
            id = msgId,
            chatId = chatId,
            uploadTimestamp = time,
            state = state.value,
            filePath = filePath,
        )
        val expected = PendingFileAttachmentMessage(
            chatId = chatId,
            msgId = msgId,
            time = time,
            isDeletable = false,
            isEditable = false,
            userHandle = userHandle,
            shouldShowAvatar = false,
            reactions = emptyList(),
            status = getChatMessageStatus(state),
            content = null,
            file = File(filePath),
            fileType = fileTypeInfo,
            isError = state == PendingMessageState.ERROR_ATTACHING || state == PendingMessageState.ERROR_UPLOADING,
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
        val fileTypeInfo = mock<UnknownFileTypeInfo>()
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
        )
        val expected = PendingVoiceClipMessage(
            chatId = chatId,
            msgId = msgId,
            time = time,
            isDeletable = false,
            isEditable = false,
            userHandle = userHandle,
            shouldShowAvatar = false,
            reactions = emptyList(),
            status = getChatMessageStatus(state),
            content = null,
            fileType = fileTypeInfo,
            isError = state == PendingMessageState.ERROR_ATTACHING || state == PendingMessageState.ERROR_UPLOADING,
        )
        val actual = underTest(pendingMessage)
        assertThat(actual).isEqualTo(expected)
    }

    private fun getChatMessageStatus(state: PendingMessageState) =
        when (state) {
            PendingMessageState.ATTACHING -> ChatMessageStatus.SENDING
            PendingMessageState.SENT -> ChatMessageStatus.DELIVERED
            PendingMessageState.ERROR_ATTACHING -> ChatMessageStatus.SERVER_REJECTED
            else -> ChatMessageStatus.UNKNOWN
        }
}
