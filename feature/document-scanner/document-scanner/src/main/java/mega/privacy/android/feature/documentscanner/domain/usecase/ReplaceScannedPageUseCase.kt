package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import javax.inject.Inject

/**
 * Replaces an existing page with a freshly captured one, preserving its position.
 * Used by the retake flow.
 */
class ReplaceScannedPageUseCase @Inject constructor(
    private val scanSessionRepository: ScanSessionRepository,
) {
    /**
     * @param pageId id of the page being retaken.
     * @param newPage the replacement page.
     */
    suspend operator fun invoke(pageId: String, newPage: ScannedPage) =
        scanSessionRepository.replacePage(pageId, newPage)
}
