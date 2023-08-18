package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.files.PdfRepository
import java.io.File
import javax.inject.Inject

/**
 * Create pdf preview use case.
 *
 * @property pdfRepository [PdfRepository]
 */
class CreatePdfPreviewUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
) {
    /**
     * Invoke.
     *
     * @param preview File in which the preview will be created.
     * @param localPath Local path required for creating a PdfDocument.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend operator fun invoke(preview: File, localPath: String) =
        pdfRepository.createPreview(preview, localPath)
}