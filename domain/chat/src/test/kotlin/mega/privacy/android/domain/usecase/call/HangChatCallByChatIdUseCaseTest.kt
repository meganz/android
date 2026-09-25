package mega.privacy.android.domain.usecase.call

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HangChatCallByChatIdUseCaseTest {
    private lateinit var underTest: HangChatCallByChatIdUseCase
    private val callRepository = mock<CallRepository>()

    @BeforeEach
    fun setUp() {
        underTest = HangChatCallByChatIdUseCase(callRepository)
    }

    @Test
    fun `test that hangChatCall is called and returns ChatCall when chatId is valid`() = runTest {
        val chatId = 123L
        val callId = 456L
        val chatCall = mock<ChatCall> {
            on { this.callId } doReturn callId
        }
        whenever(callRepository.getChatCall(chatId)).thenReturn(chatCall)
        whenever(callRepository.hangChatCall(callId)).thenReturn(mock())

        val result = underTest(chatId)

        verify(callRepository).getChatCall(chatId)
        verify(callRepository).hangChatCall(callId)
        assertThat(result).isEqualTo(chatCall)
    }

    @Test
    fun `test that hangChatCall is not called and returns null when chatId is invalid`() = runTest {
        val chatId = -1L
        val result = underTest(chatId)
        assertThat(result).isNull()
    }

    @Test
    fun `test that hangChatCall is not called and returns null when getChatCall returns null`() =
        runTest {
            val chatId = 123L
            whenever(callRepository.getChatCall(chatId)).thenReturn(null)
            val result = underTest(chatId)
            verify(callRepository).getChatCall(chatId)
            assertThat(result).isNull()
        }

    @AfterEach
    fun tearDown() {
        reset(callRepository)
    }
}