package mega.privacy.android.domain.usecase.chat.message.delete

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsMessageDeletableUseCaseTest {

    private lateinit var underTest: IsMessageDeletableUseCase

    private val chatRepository = mock<ChatRepository>()

    private val chatId = 1L
    private val msgId = 2L

    @BeforeAll
    fun setup() {
        underTest = IsMessageDeletableUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that use case returns false if message is null`() = runTest {
        whenever(chatRepository.getMessage(chatId, msgId)).thenReturn(null)

        Truth.assertThat(underTest(chatId, msgId)).isFalse()
    }

    @ParameterizedTest(name = " if isDeletable message property is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that use case returns correctly`(
        isDeletable: Boolean,
    ) = runTest {
        val message = mock<ChatMessage> {
            on { this.isDeletable } doReturn isDeletable
        }
        whenever(chatRepository.getMessage(chatId, msgId)).thenReturn(message)

        Truth.assertThat(underTest(chatId, msgId)).isEqualTo(isDeletable)
    }
}