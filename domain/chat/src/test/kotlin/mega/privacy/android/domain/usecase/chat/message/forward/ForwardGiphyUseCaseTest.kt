package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.usecase.chat.message.SendGiphyMessageUseCase
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
class ForwardGiphyUseCaseTest {

    private lateinit var underTest: ForwardGiphyUseCase

    private val sendGiphyMessageUseCase = mock<SendGiphyMessageUseCase>()

    private val targetChatId = 789L

    @BeforeEach
    fun setup() {
        underTest = ForwardGiphyUseCase(sendGiphyMessageUseCase = sendGiphyMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(sendGiphyMessageUseCase)
    }

    @Test
    fun `test that empty is returned if message is not a giphy`() = runTest {
        val message = mock<NormalMessage>()
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

    @Test
    fun `test that send giphy message use case is invoked and success is returned`() = runTest {
        val srcMp4 = "srcMp4"
        val srcWebp = "srcWebp"
        val sizeMp4 = 350
        val sizeWebp = 250
        val width = 250
        val height = 500
        val title = "title"
        val chatGifInfo = mock<ChatGifInfo> {
            on { mp4Src } doReturn srcMp4
            on { webpSrc } doReturn srcWebp
            on { mp4Size } doReturn sizeMp4
            on { webpSize } doReturn sizeWebp
            on { this.width } doReturn width
            on { this.height } doReturn height
            on { this.title } doReturn title
        }
        val message = mock<GiphyMessage> {
            on { this.chatGifInfo } doReturn chatGifInfo
        }
        whenever(
            sendGiphyMessageUseCase(
                chatId = targetChatId,
                srcMp4 = srcMp4,
                srcWebp = srcWebp,
                sizeMp4 = sizeMp4.toLong(),
                sizeWebp = sizeWebp.toLong(),
                width = width,
                height = height,
                title = title
            )
        ).thenReturn(Unit)
        underTest.invoke(listOf(targetChatId), message)
        verify(sendGiphyMessageUseCase).invoke(
            chatId = targetChatId,
            srcMp4 = srcMp4,
            srcWebp = srcWebp,
            sizeMp4 = sizeMp4.toLong(),
            sizeWebp = sizeWebp.toLong(),
            width = width,
            height = height,
            title = title
        )
        assertThat(underTest.invoke(listOf(targetChatId), message))
            .isEqualTo(listOf(ForwardResult.Success(targetChatId)))
    }
}