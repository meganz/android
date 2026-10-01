package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.files.PdfRepository
import javax.inject.Inject

/**
 * Create pdf preview use case.
 *
 * @property pdfRepository [PdfRepository]
 * @property setPreviewUseCase [SetPreviewUseCase]
 */
class CreatePdfPreviewUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val setPreviewUseCase: SetPreviewUseCase,
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
            pdfRepository.createPreview(nodeHandle, uriPath)
        }.getOrNull()?.let { previewPath ->
            setPreviewUseCase(nodeHandle = nodeHandle, srcFilePath = previewPath)
        }
}