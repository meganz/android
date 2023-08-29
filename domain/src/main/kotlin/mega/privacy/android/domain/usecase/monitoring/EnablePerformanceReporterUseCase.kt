package mega.privacy.android.domain.usecase.monitoring

import mega.privacy.android.domain.repository.monitoring.PerformanceReporterRepository
import javax.inject.Inject

/**
 * EnablePerformanceReporterUseCase
 */
class EnablePerformanceReporterUseCase @Inject constructor(
    private val performanceReporterRepository: PerformanceReporterRepository,
) {
    /**
     *
     * @param enabled [Boolean]
     */
    operator fun invoke(enabled: Boolean) =
        performanceReporterRepository.setEnabled(enabled)
}
