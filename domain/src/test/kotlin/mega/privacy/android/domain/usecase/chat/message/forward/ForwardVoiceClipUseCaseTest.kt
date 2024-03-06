package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.chat.message.AttachVoiceClipMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardNodeAttachmentUseCase.Companion.API_ENOENT
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardVoiceClipUseCaseTest {

    private lateinit var underTest: ForwardVoiceClipUseCase

    private val attachVoiceClipMessageUseCase = mock<AttachVoiceClipMessageUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()

    private val targetChatId = 789L
    private val node = mock<ChatDefaultFile>()
    private val message = mock<VoiceClipMessage> {
        on { chatId } doReturn 123L
        on { msgId } doReturn 456L
    }

    @BeforeEach
    fun setup() {
        underTest = ForwardVoiceClipUseCase(
            attachVoiceClipMessageUseCase = attachVoiceClipMessageUseCase,
            getChatFileUseCase = getChatFileUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(attachVoiceClipMessageUseCase, getChatFileUseCase)
    }

    @Test
    fun `test that empty is returned if message is not a voice clip`() = runTest {
        val message = mock<NormalMessage>()
        underTest.invoke(listOf(targetChatId), message)

        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

    @Test
    fun `test that general error is returned if attach request throws a general exception`() =
        runTest {
            whenever(getChatFileUseCase(message.chatId, message.msgId)).thenReturn(node)
            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenAnswer {
                throw MegaException(errorCode = -1, null)
            }

            assertThat(underTest.invoke(listOf(targetChatId), message))
                .isEqualTo(listOf(ForwardResult.GeneralError))
        }

    @Test
    fun `test that not available error is returned if attach request throws an API_ENOENT exception`() =
        runTest {
            whenever(getChatFileUseCase(message.chatId, message.msgId)).thenReturn(node)
            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenAnswer {
                throw MegaException(errorCode = API_ENOENT, null)
            }

            assertThat(underTest.invoke(listOf(targetChatId), message))
                .isEqualTo(listOf(ForwardResult.ErrorNotAvailable))
        }

    @Test
    fun `test that attach voice clip message use case is invoked and success is returned`() =
        runTest {
            whenever(getChatFileUseCase(message.chatId, message.msgId)).thenReturn(node)
            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenReturn(Unit)

            assertThat(underTest.invoke(listOf(targetChatId), message))
                .isEqualTo(listOf(ForwardResult.Success(targetChatId)))
        }
}