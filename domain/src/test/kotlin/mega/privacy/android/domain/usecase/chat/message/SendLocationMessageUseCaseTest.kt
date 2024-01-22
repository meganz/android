package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
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
    private val createMetaMessageUseCase = mock<CreateMetaMessageUseCase>()

    @BeforeEach
    fun setup() {
        underTest = SendLocationMessageUseCase(chatRepository, createMetaMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository, createMetaMessageUseCase)
    }

    @Test
    fun `test that use case returns expected message and verify ChatRepository and CreateMetaMessageUseCase are invoked`() =
        runTest {
            val chatId = 123L
            val longitude = 1.0F
            val latitude = 1.0F
            val image = "image"
            val message = mock<ChatMessage>()
            val request = CreateTypedMessageRequest(
                chatMessage = message,
                isMine = true,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false
            )
            val expectedMessage = mock<MetaMessage>()
            whenever(chatRepository.sendGeolocation(chatId, longitude, latitude, image))
                .thenReturn(message)
            whenever(createMetaMessageUseCase(request)).thenReturn(expectedMessage)
            Truth.assertThat(underTest.invoke(chatId, longitude, latitude, image))
                .isEqualTo(expectedMessage)
            verify(chatRepository).sendGeolocation(chatId, longitude, latitude, image)
            verify(createMetaMessageUseCase).invoke(request)
        }
}