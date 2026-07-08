package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import javax.inject.Inject

/**
 * Removes a page from the current scan session by its id.
 */
class RemoveScannedPageUseCase @Inject constructor(
    private val scanSessionRepository: ScanSessionRepository,
) {
    /**
     * @param pageId id of the page to remove.
     */
    suspend operator fun invoke(pageId: String) = scanSessionRepository.removePage(pageId)
}
