package mega.privacy.android.domain.usecase.monitoring

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.monitoring.PerformanceReporterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [StartTracePerformanceUseCaseTest]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartTracePerformanceUseCaseTest {

    private lateinit var underTest: StartTracePerformanceUseCase

    private val performanceReporterRepository = mock<PerformanceReporterRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StartTracePerformanceUseCase(
            performanceReporterRepository = performanceReporterRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(performanceReporterRepository)
    }

    @Test
    fun `test that performance is traced when invoked`() =
        runTest {
            val value = 2
            val block = suspend { value }
            val name = "traceName"
            whenever(performanceReporterRepository.trace(name, block)).thenReturn(value)
            val actual = underTest(name, block)
            verify(performanceReporterRepository).setEnabled(true)
            Truth.assertThat(actual).isEqualTo(value)
        }
}
