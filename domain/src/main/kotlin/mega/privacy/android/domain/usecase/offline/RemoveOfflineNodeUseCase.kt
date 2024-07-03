package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to delete offline nodes
 * @property nodeRepository [NodeRepository]
 * @property fileRepository [FileSystemRepository]
 * @property ioDispatcher [CoroutineDispatcher]
 */
class RemoveOfflineNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val fileRepository: FileSystemRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * invoke
     * @param nodeId [NodeId]
     */
    suspend operator fun invoke(nodeId: NodeId) = withContext(ioDispatcher) {
        nodeRepository.getOfflineNodeInformation(nodeId)?.let {
            if (it.isFolder) {
                val childNodes = nodeRepository.getOfflineNodesByParentId(it.id)
                deleteChildrenInDb(childNodes)
            }
            //remove red arrow from current item
            val parentId = it.parentId
            nodeRepository.removeOfflineNodeById(it.id)
            if (parentId != -1) {
                updateParentOfflineStatus(parentId)
            }
            val path = when (it) {
                is BackupsOfflineNodeInformation -> fileRepository.getOfflineBackupsPath()
                else -> fileRepository.getOfflinePath()
            }
            deleteFileFromPath(path = "$path${it.path}${it.name}")
        }
    }

    private suspend fun deleteFileFromPath(path: String) {
        fileRepository.deleteFolderAndItsFiles(path)
    }

    private suspend fun deleteChildrenInDb(offlineInfoChildren: List<OfflineNodeInformation>?) {
        var offlineChild: OfflineNodeInformation
        offlineInfoChildren?.forEach {
            offlineChild = it
            val children = nodeRepository.getOfflineNodesByParentId(offlineChild.id)
            if (offlineInfoChildren.isNotEmpty()) {
                deleteChildrenInDb(children)
            }
            nodeRepository.removeOfflineNodeById(offlineChild.id)
        }
    }

    private suspend fun updateParentOfflineStatus(parentId: Int) {
        val siblings = nodeRepository.getOfflineNodesByParentId(parentId)
        siblings?.let {
            if (it.isNotEmpty()) {
                return
            }
            val parentNode = nodeRepository.getOfflineNodeById(parentId)
            parentNode?.let { offlineInfo ->
                val grandParentId = offlineInfo.parentId
                nodeRepository.removeOfflineNodeById(parentId)
                updateParentOfflineStatus(grandParentId)
            }
        }
    }
}