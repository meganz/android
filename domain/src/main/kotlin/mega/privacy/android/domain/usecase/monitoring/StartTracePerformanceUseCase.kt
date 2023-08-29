package mega.privacy.android.domain.usecase.monitoring

import mega.privacy.android.domain.repository.monitoring.PerformanceReporterRepository
import javax.inject.Inject

/**
 * TracePerformanceUseCase
 */
class StartTracePerformanceUseCase @Inject constructor(
    private val performanceReporterRepository: PerformanceReporterRepository,
) {
    /**
     * Measures the time it takes to run the [block]
     *
     * @param traceName     Trace name to be uniquely identified
     */
    suspend operator fun <T> invoke(traceName: String, block: suspend () -> T): T {
        performanceReporterRepository.setEnabled(true)
        return performanceReporterRepository.trace(traceName, block)
    }
}
