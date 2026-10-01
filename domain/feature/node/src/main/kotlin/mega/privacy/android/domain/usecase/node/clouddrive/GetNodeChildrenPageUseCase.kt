package mega.privacy.android.domain.usecase.node.clouddrive

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodePage
import mega.privacy.android.domain.entity.node.PageRequest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import javax.inject.Inject

/**
 * One page of a folder's children, typed and in the given order
 *
 * @property nodeRepository
 * @property getFolderTypeDataUseCase
 */
class GetNodeChildrenPageUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeId the parent folder
     * @param sortOrder the order the SDK applies before slicing the page
     * @param excludeSensitives when true, the SDK search filter excludes sensitive (hidden) nodes
     * @param page the offset and size of the page
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        sortOrder: SortOrder,
        excludeSensitives: Boolean,
        page: PageRequest,
    ): NodePage<TypedNode> = nodeRepository.getTypedChildrenPage(
        nodeId = nodeId,
        order = sortOrder,
        page = page,
        folderTypeData = getFolderTypeDataUseCase(),
        sensitivityFilter = SensitivityFilterOption.NonSensitiveOnly.takeIf { excludeSensitives },
    )
}
