package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import javax.inject.Inject

/**
 * Moves a page within the current scan session, re-indexing the rest.
 */
class ReorderScannedPagesUseCase @Inject constructor(
    private val scanSessionRepository: ScanSessionRepository,
) {
    /**
     * @param fromIndex current position of the page.
     * @param toIndex target position of the page.
     */
    suspend operator fun invoke(fromIndex: Int, toIndex: Int) =
        scanSessionRepository.reorderPages(fromIndex, toIndex)
}
