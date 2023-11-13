package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnableAudioLevelMonitorUseCaseTest {
    private lateinit var underTest: EnableAudioLevelMonitorUseCase
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = EnableAudioLevelMonitorUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = " enable {0}")
    @ValueSource(booleans = [true, false])
    fun `test that enable audio level monitor invokes the repository correctly when`(
        enable: Boolean,
    ) {
        runTest {
            val chatId = 1234L
            whenever(chatRepository.enableAudioLevelMonitor(enable, chatId)).thenReturn(Unit)
            underTest.invoke(enable, chatId)
            verify(chatRepository).enableAudioLevelMonitor(enable, chatId)
            verifyNoMoreInteractions(chatRepository)
        }
    }
}