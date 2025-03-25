package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.files.PdfRepository
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
    suspend operator fun invoke(nodeHandle: Long, uriPath: UriPath) =
        runCatching {
            pdfRepository.createThumbnail(nodeHandle, uriPath)
        }.getOrNull()?.let { thumbnailPath ->
            setThumbnailUseCase(nodeHandle = nodeHandle, srcFilePath = thumbnailPath)
        }
}