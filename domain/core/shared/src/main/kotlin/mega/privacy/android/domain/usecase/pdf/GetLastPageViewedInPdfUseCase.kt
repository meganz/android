package mega.privacy.android.domain.usecase.pdf

import mega.privacy.android.domain.repository.files.PdfRepository
import javax.inject.Inject

/**
 * Use case to get the last page viewed in a PDF document.
 */
class GetLastPageViewedInPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(nodeHandle: Long) =
        pdfRepository.getLastPageViewedInPdf(nodeHandle)
}