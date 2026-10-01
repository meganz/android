package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAudioOutputUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private lateinit var underTest: MonitorAudioOutputUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorAudioOutputUseCase(callRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        whenever(callRepository.monitorAudioOutput()).thenReturn(flowOf(AudioDevice.SpeakerPhone))
        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(AudioDevice.SpeakerPhone)
            cancelAndIgnoreRemainingEvents()
        }
    }
}