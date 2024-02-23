package mega.privacy.android.domain.usecase.chat.message.edit

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditLocationMessageUseCaseTest {

    private lateinit var underTest: EditLocationMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setUp() {
        underTest = EditLocationMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that edit location message use case invokes and returns correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val longitude = 3.0f
        val latitude = 4.0f
        val image = "image"
        val chatMessage = mock<ChatMessage>()
        whenever(chatMessageRepository.editGeolocation(chatId, msgId, longitude, latitude, image))
            .thenReturn(chatMessage)
        Truth.assertThat(underTest.invoke(chatId, msgId, longitude, latitude, image))
            .isEqualTo(chatMessage)
    }
}