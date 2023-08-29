package mega.privacy.android.domain.usecase.monitoring

import mega.privacy.android.domain.repository.monitoring.PerformanceReporterRepository
import javax.inject.Inject

/**
 * StopTracePerformanceUseCase
 */
class StopTracePerformanceUseCase @Inject constructor(
    private val performanceReporterRepository: PerformanceReporterRepository,
) {
    /**
     *
     * @param traceNames     Trace name to be uniquely identified
     */
    operator fun invoke(traceNames: List<String>) =
        performanceReporterRepository.stopTraces(traceNames)
}
