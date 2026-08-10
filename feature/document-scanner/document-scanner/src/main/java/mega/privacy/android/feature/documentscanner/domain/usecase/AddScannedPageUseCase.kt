package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import javax.inject.Inject

/**
 * Adds a captured [ScannedPage] to the current scan session.
 */
class AddScannedPageUseCase @Inject constructor(
    private val scanSessionRepository: ScanSessionRepository,
) {
    /**
     * @param page the captured page to append to the session.
     */
    suspend operator fun invoke(page: ScannedPage) = scanSessionRepository.addPage(page)
}
