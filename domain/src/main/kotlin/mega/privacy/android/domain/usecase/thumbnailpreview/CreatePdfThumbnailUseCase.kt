package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.files.PdfRepository
import java.io.File
import javax.inject.Inject

/**
 * Create pdf thumbnail use case.
 *
 * @property pdfRepository [PdfRepository]
 */
class CreatePdfThumbnailUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
) {
    /**
     * Invoke.
     *
     * @param thumbnail File in which the thumbnail will be created.
     * @param localPath Local path required for creating a PdfDocument.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend operator fun invoke(thumbnail: File, localPath: String) =
        pdfRepository.createThumbnail(thumbnail, localPath)
}