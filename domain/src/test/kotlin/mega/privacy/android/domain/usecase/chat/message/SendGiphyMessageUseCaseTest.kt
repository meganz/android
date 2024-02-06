package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendGiphyMessageUseCaseTest {

    private lateinit var underTest: SendGiphyMessageUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()

    private val chatId = 123L
    private val srcMp4 = "srcMp4"
    private val srcWebp = "srcWebp"
    private val sizeMp4 = 350L
    private val sizeWebp = 250L
    private val width = 250
    private val height = 500
    private val title = "title"

    @BeforeEach
    fun setup() {
        underTest = SendGiphyMessageUseCase(
            chatRepository,
            chatMessageRepository,
            createSaveSentMessageRequestUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatRepository,
            chatMessageRepository,
            createSaveSentMessageRequestUseCase,
        )
    }

    @Test
    fun `test that message is sent`() =
        runTest {
            val message = mock<ChatMessage>()
            whenever(
                chatMessageRepository.sendGiphy(
                    chatId = chatId,
                    srcMp4 = srcMp4,
                    srcWebp = srcWebp,
                    sizeMp4 = sizeMp4,
                    sizeWebp = sizeWebp,
                    width = width,
                    height = height,
                    title = title
                )
            ).thenReturn(message)
            underTest.invoke(
                chatId = chatId,
                srcMp4 = srcMp4,
                srcWebp = srcWebp,
                sizeMp4 = sizeMp4,
                sizeWebp = sizeWebp,
                width = width,
                height = height,
                title = title
            )
            verify(chatMessageRepository).sendGiphy(
                chatId = chatId,
                srcMp4 = srcMp4,
                srcWebp = srcWebp,
                sizeMp4 = sizeMp4,
                sizeWebp = sizeWebp,
                width = width,
                height = height,
                title = title
            )
        }


    @Test
    fun `test that message is stored`() = runTest {
        val message = mock<ChatMessage>()
        whenever(
            chatMessageRepository.sendGiphy(
                chatId = chatId,
                srcMp4 = srcMp4,
                srcWebp = srcWebp,
                sizeMp4 = sizeMp4,
                sizeWebp = sizeWebp,
                width = width,
                height = height,
                title = title
            )
        ).thenReturn(message)
        val request = mock<CreateTypedMessageRequest>()
        whenever(createSaveSentMessageRequestUseCase(message)).thenReturn(request)
        underTest.invoke(
            chatId = chatId,
            srcMp4 = srcMp4,
            srcWebp = srcWebp,
            sizeMp4 = sizeMp4,
            sizeWebp = sizeWebp,
            width = width,
            height = height,
            title = title
        )
        verify(chatRepository).storeMessages(chatId, listOf(request))
    }
}