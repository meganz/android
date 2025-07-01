package mega.privacy.android.domain.usecase.pdf

import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.files.PdfRepository
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import javax.inject.Inject

/**
 * Use case to delete the last page viewed in a PDF document.
 *
 * Normally, this is used when the PDF is removed from Cloud or Offline.
 */
class CheckIfShouldDeleteLastPageViewedInPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val nodeRepository: NodeRepository,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(nodeHandle: Long, fileName: String, isOfflineRemoval: Boolean) {
        getFileTypeInfoByNameUseCase(fileName).let { fileTypeInfo ->
            if (fileTypeInfo is PdfFileTypeInfo) {
                if ((isOfflineRemoval && nodeRepository.getNodeById(NodeId(nodeHandle)) == null)
                    || isOfflineRemoval.not() && nodeRepository.getOfflineNodeInformation(nodeHandle) == null
                ) {
                    pdfRepository.deleteLastPageViewedInPdf(nodeHandle)
                }
            }
        }
    }
}