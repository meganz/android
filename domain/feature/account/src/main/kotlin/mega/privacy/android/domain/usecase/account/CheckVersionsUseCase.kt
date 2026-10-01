package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Launches a request to check versions
 */
class CheckVersionsUseCase @Inject constructor(private val nodeRepository: NodeRepository) {

    /**
     * Invoke
     * @return [FolderTreeInfo]
     */
    suspend operator fun invoke(): FolderTreeInfo? {
        val rootNode = nodeRepository.getRootNode()
        return (rootNode as? FolderNode)?.let {
            nodeRepository.getFolderTreeInfo(it)
        }
    }
}