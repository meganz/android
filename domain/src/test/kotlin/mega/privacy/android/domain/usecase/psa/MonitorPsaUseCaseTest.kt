package mega.privacy.android.domain.usecase.psa

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.repository.psa.PsaRepository
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorPsaUseCaseTest {
    lateinit var underTest: MonitorPsaUseCase

    private val monitorMiscLoadedUseCase = mock<MonitorMiscLoadedUseCase>()

    private val psaRepository = mock<PsaRepository>()

    private val refreshPeriod = 10

    @BeforeEach
    fun setUp() {
        underTest = MonitorPsaUseCase(
            psaRepository = psaRepository,
            monitorMiscLoadedUseCase = monitorMiscLoadedUseCase,
            psaCheckFrequency = refreshPeriod.seconds,
        )
    }

    @Test
    fun `test that refresh use case is called`() = runTest {
        monitorMiscLoadedUseCase.stub {
            on { invoke() }.thenReturn(kotlinx.coroutines.flow.flow {
                emit(true)
            })
        }
        underTest.invoke().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(psaRepository).refreshPsa()
    }

    @Test
    fun `test that refresh use case is called after every check frequency`() = runTest {
        monitorMiscLoadedUseCase.stub {
            on { invoke() }.thenReturn(kotlinx.coroutines.flow.flow {
                emit(true)
            })
        }

        psaRepository.stub {
            on { monitorPsa() }.thenReturn(kotlinx.coroutines.flow.flow {
                emit(getPsa(1))
                awaitCancellation()
            })
        }

        underTest.invoke().test {
            verify(psaRepository).refreshPsa()
            testScheduler.advanceTimeBy((refreshPeriod * 5).seconds)
            cancelAndConsumeRemainingEvents()
            verify(psaRepository, times(5)).refreshPsa()
        }
    }

    private fun getPsa(id: Int) = Psa(
        id = id,
        title = "title",
        text = "description",
        imageUrl = null,
        positiveText = null,
        positiveLink = null,
        url = null
    )
}