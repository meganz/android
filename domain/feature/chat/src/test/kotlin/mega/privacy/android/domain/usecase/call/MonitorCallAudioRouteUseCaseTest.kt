package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.AudioRoute
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCallAudioRouteUseCaseTest {

    private lateinit var underTest: MonitorCallAudioRouteUseCase

    private val callAudioRouteRepository = mock<CallAudioRouteRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCallAudioRouteUseCase(
            callAudioRouteRepository = callAudioRouteRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(callAudioRouteRepository)
    }

    @Test
    fun `test that invoke emits the routes from the repository`() = runTest {
        val idle = AudioRoute(current = AudioDevice.None, available = emptySet())
        val active = AudioRoute(
            current = AudioDevice.Earpiece,
            available = setOf(AudioDevice.Earpiece, AudioDevice.SpeakerPhone),
        )
        whenever(callAudioRouteRepository.monitorAudioRoute()).thenReturn(flow {
            emit(idle)
            emit(active)
            awaitCancellation()
        })

        underTest().test {
            assertThat(awaitItem()).isEqualTo(idle)
            assertThat(awaitItem()).isEqualTo(active)
        }
    }
}
