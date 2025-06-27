package mega.privacy.android.domain.usecase.pdf

import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.repository.files.PdfRepository
import javax.inject.Inject

/**
 * Use case to set or update the last page viewed in a PDF document.
 */
class SetOrUpdateLastPageViewedInPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(lastPageViewedInPdf: LastPageViewedInPdf) {
        pdfRepository.setOrUpdateLastPageViewedInPdf(lastPageViewedInPdf)
    }
}