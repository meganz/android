package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Map node to public link use case
 *
 * @property addNodeType
 * @property getCloudSortOrder
 * @constructor Create empty Map node to public link use case
 */
class MapNodeToPublicLinkUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * Invoke
     *
     * @param node
     * @param parent
     * @return
     */
    suspend operator fun invoke(node: UnTypedNode, parent: PublicLinkFolder?): PublicLinkNode =
        when (val typedNode = addNodeType(node)) {
            is TypedFileNode -> PublicLinkFile(typedNode, parent)
            is TypedFolderNode -> PublicLinkFolder(
                node = typedNode,
                parent = parent,
                fetchChildrenForParent = getFetchMethod(typedNode.fetchChildren)
            )
        }

    private fun getFetchMethod(fetchChildren: suspend (SortOrder) -> List<UnTypedNode>): suspend (PublicLinkFolder) -> List<PublicLinkNode> =
        { parentFolder ->
            val children = fetchChildren(getCloudSortOrder())
            children.map {
                invoke(
                    node = it,
                    parent = parentFolder
                )
            }
        }
}