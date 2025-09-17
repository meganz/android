package mega.privacy.android.domain.usecase.filebrowser

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import javax.inject.Inject

/**
 * Use case to get children nodes of a folder
 */
class GetFileBrowserNodeChildrenUseCase @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase,
) {

    /**
     * Get children nodes of the browser parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<TypedNode> = coroutineScope {
        val sortOrderDiffer = async { getCloudSortOrder() }
        val folderTypeDataDiffer = async { getFolderTypeDataUseCase() }
        val nodeId = (if (parentHandle != nodeRepository.getInvalidHandle()) {
            NodeId(parentHandle)
        } else {
            getRootNodeIdUseCase()
        }) ?: return@coroutineScope emptyList()

        nodeRepository.getTypedNodesById(
            nodeId = nodeId,
            order = sortOrderDiffer.await(),
            folderTypeData = folderTypeDataDiffer.await()
        )
    }
}