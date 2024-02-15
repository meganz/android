package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardRichPreviewUseCaseTest {

    private lateinit var underTest: ForwardRichPreviewUseCase

    private val sendTextMessageUseCase = mock<SendTextMessageUseCase>()

    private val targetChatId = 789L

    @BeforeEach
    fun setup() {
        underTest = ForwardRichPreviewUseCase(sendTextMessageUseCase = sendTextMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(sendTextMessageUseCase)
    }

    @Test
    fun `test that empty is returned if message is not a normal message`() = runTest {
        val message = mock<GiphyMessage>()
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

    @Test
    fun `test that send text message use case is invoked and success is returned`() = runTest {
        val content = "content"
        val message = mock<RichPreviewMessage> {
            on { this.content } doReturn content
        }
        whenever(sendTextMessageUseCase(targetChatId, content))
            .thenReturn(Unit)
        underTest.invoke(listOf(targetChatId), message)
        verify(sendTextMessageUseCase).invoke(targetChatId, content)
        assertThat(underTest.invoke(listOf(targetChatId), message))
            .isEqualTo(listOf(ForwardResult.Success(targetChatId)))
    }
}