package mega.privacy.android.domain.usecase.pdf

import mega.privacy.android.domain.repository.files.PdfRepository
import javax.inject.Inject

/**
 * Use case to delete the last page viewed in a PDF document.
 *
 * Normally, this is used when the PDF is removed from Cloud.
 */
class DeleteLastPageViewedInPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(nodeHandle: Long) {
        pdfRepository.deleteLastPageViewedInPdf(nodeHandle)
    }
}