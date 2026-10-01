package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
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
class IsAudioLevelMonitorEnabledUseCaseTest {
    private lateinit var underTest: IsAudioLevelMonitorEnabledUseCase
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsAudioLevelMonitorEnabledUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = " enabled {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is audio level monitor enabled returns correctly if repository returns`(
        enabled: Boolean,
    ) {
        runTest {
            val chatId = 1234L
            whenever(chatRepository.isAudioLevelMonitorEnabled(chatId)).thenReturn(enabled)
            Truth.assertThat(underTest.invoke(chatId)).isEqualTo(enabled)
            verify(chatRepository).isAudioLevelMonitorEnabled(chatId)
            verifyNoMoreInteractions(chatRepository)
        }
    }
}