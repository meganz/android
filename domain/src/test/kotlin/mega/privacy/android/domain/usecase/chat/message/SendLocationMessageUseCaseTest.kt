package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendLocationMessageUseCaseTest {

    private lateinit var underTest: SendLocationMessageUseCase

    private val chatRepository = mock<ChatRepository>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()

    private val chatId = 123L
    private val longitude = 1.0F
    private val latitude = 1.0F
    private val image = "image"

    @BeforeEach
    fun setup() {
        underTest = SendLocationMessageUseCase(
            chatRepository,
            createSaveSentMessageRequestUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository, createSaveSentMessageRequestUseCase)
    }

    @Test
    fun `test that message is sent`() =
        runTest {
            val message = mock<ChatMessage>()
            whenever(chatRepository.sendGeolocation(chatId, longitude, latitude, image))
                .thenReturn(message)
            underTest.invoke(chatId, longitude, latitude, image)
            verify(chatRepository).sendGeolocation(chatId, longitude, latitude, image)
        }


    @Test
    fun `test that message is stored`() = runTest {
        val sentMessage = mock<ChatMessage>()
        whenever(chatRepository.sendGeolocation(chatId, longitude, latitude, image))
            .thenReturn(sentMessage)
        val request = mock<CreateTypedMessageRequest>()
        whenever(createSaveSentMessageRequestUseCase(sentMessage)).thenReturn(request)
        underTest.invoke(chatId, longitude, latitude, image)
        verify(chatRepository).storeMessages(chatId, listOf(request))
    }
}