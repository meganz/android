package mega.privacy.android.domain.usecase.monitoring

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

/**
 * Test class for [StopTracePerformanceUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StopTracePerformanceUseCaseTest {

    private lateinit var underTest: StopTracePerformanceUseCase

    private val performanceReporterRepository = mock<PerformanceReporterRepository>()

    @BeforeAll
    fun setUp() {
        underTest = StopTracePerformanceUseCase(
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
            val names = listOf("traceName")
            underTest(names)
            verify(performanceReporterRepository).stopTraces(names)
        }
}
