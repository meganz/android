package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByNodeIdUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to get local file of a node
 *
 * if node is available offline use offline node for preview
 * if local file does not exists checks if there is a preview file in cache
 */
class GetNodePreviewFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val cacheRepository: CacheRepository,
    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
) {

    /**
     * Invoke
     * @param node [Node]
     */
    suspend operator fun invoke(node: TypedFileNode): File? {
        var file: File? = null
        if (node.isAvailableOffline) {
            getOfflineNodeInformationByNodeIdUseCase(node.id)?.let {
                file = getOfflineFileUseCase(it)
            }
        }
        return file ?: fileSystemRepository.getLocalFile(node)
        ?: cacheRepository.getPreviewFile(node.name)
    }
}