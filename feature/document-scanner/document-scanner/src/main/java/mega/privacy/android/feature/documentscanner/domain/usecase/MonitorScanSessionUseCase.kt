package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import javax.inject.Inject

/**
 * Observes the current scan session and its pages.
 */
class MonitorScanSessionUseCase @Inject constructor(
    private val scanSessionRepository: ScanSessionRepository,
) {
    operator fun invoke(): Flow<ScanSession> = scanSessionRepository.getSession()
}
