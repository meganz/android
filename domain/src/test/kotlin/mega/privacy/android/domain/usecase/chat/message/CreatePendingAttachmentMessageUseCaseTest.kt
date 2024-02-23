package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
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

    @BeforeEach
    internal fun setUp() {
        underTest = CreatePendingAttachmentMessageUseCase(getMyUserHandleUseCase)
    }

    @AfterEach
    internal fun resetMocks() {
        reset(getMyUserHandleUseCase)
    }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that PendingAttachmentMessage has correct values`(
        state: PendingMessageState,
    ) = runTest {
        val chatId = 156L
        val msgId = 87L
        val time = 72834578L
        val userHandle = 245L
        val filePath = "filepath"
        whenever(getMyUserHandleUseCase()).thenReturn(userHandle)
        val pendingMessage = PendingMessage(
            id = msgId,
            chatId = chatId,
            uploadTimestamp = time,
            state = state.value,
            filePath = filePath,
        )
        val expected = PendingAttachmentMessage(
            chatId = chatId,
            msgId = msgId,
            time = time,
            isDeletable = false,
            isEditable = false,
            userHandle = userHandle,
            shouldShowAvatar = true,
            shouldShowTime = true,
            reactions = emptyList(),
            status = getChatMessageStatus(state),
            file = File(filePath),
            fileType = UnknownFileTypeInfo("", "")
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
