package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAnotherCallParticipatingUseCaseTest {
    private lateinit var underTest: GetAnotherCallParticipatingUseCase
    private val callRepository: CallRepository = mock()
    private val chatRepository: ChatRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = GetAnotherCallParticipatingUseCase(
            callRepository,
            chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository, callRepository)
    }

    @Test
    fun `test that an invalid chat handle is returned when the call handle list is empty`() =
        runTest {
            whenever(callRepository.getCallHandleList(any())).thenReturn(emptyList())
            whenever(chatRepository.getChatInvalidHandle()).thenReturn(-1L)
            Truth.assertThat(underTest(1)).isEqualTo(-1L)
        }

    @Test
    fun `test that an invalid chat handle is returned when the call handle list only contains a single chat id`() =
        runTest {
            val chatId = 1L
            whenever(callRepository.getCallHandleList(any())).thenReturn(listOf(chatId))
            whenever(chatRepository.getChatInvalidHandle()).thenReturn(-1L)
            Truth.assertThat(underTest(chatId)).isEqualTo(-1L)
        }

    @Test
    fun `test that the correct chat handle is returned when the call handle list does not contain the chat id`() =
        runTest {
            val chatId = 1L
            whenever(callRepository.getCallHandleList(any())).thenReturn(listOf(2L, 3L))
            whenever(chatRepository.getChatInvalidHandle()).thenReturn(-1L)
            Truth.assertThat(underTest(chatId)).isEqualTo(2L)
        }
}