package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.files.PdfRepository
import java.io.File
import javax.inject.Inject

/**
 * Create pdf thumbnail use case.
 *
 * @property pdfRepository [PdfRepository]
 * @property setThumbnailUseCase [SetThumbnailUseCase]
 */
class CreatePdfThumbnailUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val setThumbnailUseCase: SetThumbnailUseCase,
) {
    /**
     * Invoke.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param localFile Local file.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend operator fun invoke(nodeHandle: Long, localFile: File) =
        runCatching {
            pdfRepository.createThumbnail(nodeHandle, localFile)
        }.getOrNull()?.let { thumbnailPath ->
            setThumbnailUseCase(nodeHandle = nodeHandle, srcFilePath = thumbnailPath)
        }
}