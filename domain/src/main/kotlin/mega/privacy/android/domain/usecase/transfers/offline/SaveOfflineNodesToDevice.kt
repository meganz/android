package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import javax.inject.Inject

/**
 * Save offline nodes to device
 *
 */
class SaveOfflineNodesToDevice @Inject constructor(
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @param destinationUri
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
        destinationUri: UriPath,
    ) {
        when {
            fileSystemRepository.isContentUri(destinationUri.value) -> {
                nodes.forEach {
                    val file = getOfflineFileUseCase(getOfflineNodeInformationUseCase(it))
                    fileSystemRepository.copyFilesToDocumentUri(file, destinationUri)
                }
            }

            fileSystemRepository.isFileUri(destinationUri.value) -> {
                val destination = fileSystemRepository.getFileFromFileUri(destinationUri.value)
                nodes.forEach {
                    val file = getOfflineFileUseCase(getOfflineNodeInformationUseCase(it))
                    fileSystemRepository.copyFiles(file, destination)
                }
            }

            fileSystemRepository.getFileByPath(destinationUri.value) != null -> {
                val destination = fileSystemRepository.getFileByPath(destinationUri.value) ?: return
                nodes.forEach {
                    val file = getOfflineFileUseCase(getOfflineNodeInformationUseCase(it))
                    fileSystemRepository.copyFiles(file, destination)
                }
            }

            else -> throw IllegalArgumentException("Invalid destination uri $destinationUri")
        }
    }
}