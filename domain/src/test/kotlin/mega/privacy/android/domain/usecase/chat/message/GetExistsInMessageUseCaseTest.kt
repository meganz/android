package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetExistsInMessageUseCaseTest {

    private lateinit var underTest: GetExistsInMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = GetExistsInMessageUseCase(
            chatMessageRepository = chatMessageRepository,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that get exists in message invokes correctly`() = runTest {
        val chatId = 123L
        val msgId = 456L
        whenever(chatMessageRepository.getExistsInMessage(chatId, msgId)).thenReturn(true)
        Truth.assertThat(underTest.invoke(chatId, msgId)).isEqualTo(true)
    }
}