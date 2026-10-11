package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByNodeIdUseCase
import javax.inject.Inject

/**
 * Save offline nodes to device
 *
 */
class SaveOfflineNodesToDevice @Inject constructor(
    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param nodeIds
     * @param destinationUri
     */
    suspend operator fun invoke(
        nodeIds: List<NodeId>,
        destinationUri: UriPath,
    ): Int {
        return when {
            fileSystemRepository.isContentUri(destinationUri.value) -> {
                nodeIds.mapNotNull {
                    getOfflineNodeInformationByNodeIdUseCase(it)
                }.sumOf { offlineInfo ->
                    val file = getOfflineFileUseCase(offlineInfo)
                    fileSystemRepository.copyFilesToDocumentUri(file, destinationUri)
                }
            }

            fileSystemRepository.isFileUri(destinationUri.value) -> {
                val destination = fileSystemRepository.getFileFromFileUri(destinationUri.value)
                nodeIds.mapNotNull {
                    getOfflineNodeInformationByNodeIdUseCase(it)
                }.sumOf { offlineInfo ->
                    val file = getOfflineFileUseCase(offlineInfo)
                    fileSystemRepository.copyFiles(file, destination)
                }
            }

            fileSystemRepository.getFileByPath(destinationUri.value) != null -> {
                val destination =
                    fileSystemRepository.getFileByPath(destinationUri.value) ?: return 0
                nodeIds.mapNotNull {
                    getOfflineNodeInformationByNodeIdUseCase(it)
                }.sumOf { offlineInfo ->
                    val file = getOfflineFileUseCase(offlineInfo)
                    fileSystemRepository.copyFiles(file, destination)
                }
            }

            else -> throw IllegalArgumentException("Invalid destination uri $destinationUri")
        }
    }
}