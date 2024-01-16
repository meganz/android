package mega.privacy.android.domain.usecase.chat.link

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorJoiningChatUseCaseTest {

    private lateinit var underTest: MonitorJoiningChatUseCase

    private val chatRepository = mock<ChatRepository>()

    private val chatId = 1L

    @BeforeAll
    internal fun setup() {
        underTest = MonitorJoiningChatUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeAll
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that repository is invoked and use case emits its values`() = runTest {
        val updatedFlow = MutableSharedFlow<Boolean>()
        whenever(chatRepository.monitorJoiningChat(chatId)).thenReturn(updatedFlow)
        underTest(chatId).test {
            updatedFlow.emit(true)
            testScheduler.advanceUntilIdle()
            Truth.assertThat(awaitItem()).isTrue()
            updatedFlow.emit(false)
            Truth.assertThat(awaitItem()).isFalse()
        }
        verify(chatRepository).monitorJoiningChat(chatId)
    }
}