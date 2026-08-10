package mega.privacy.android.domain.usecase.node.publiclink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Map typed node to public link use case
 *
 * Maps an already-typed node to its public link equivalent ([PublicLinkFile] or [PublicLinkFolder]).
 * Unlike [MapNodeToPublicLinkUseCase], this use case accepts a [TypedNode] directly, avoiding a
 * redundant [AddNodeType] call when the node is already typed.
 *
 * @property addNodeType
 * @property monitorPublicLinkFolderUseCase
 */
class MapTypedNodeToPublicLinkUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val monitorPublicLinkFolderUseCase: MonitorPublicLinkFolderUseCase,
) {
    /**
     * Invoke
     *
     * @param node already-typed node to map
     * @param parent parent [PublicLinkFolder], or null if root
     * @return [PublicLinkNode]
     */
    operator fun invoke(node: TypedNode, parent: PublicLinkFolder? = null): PublicLinkNode =
        when (node) {
            is TypedFileNode -> PublicLinkFile(node, parent)
            is TypedFolderNode -> PublicLinkFolder(
                node = node,
                parent = parent,
                monitorChildren = getFetchMethod(),
            )

            else -> throw IllegalStateException("Invalid type: $node")
        }

    private fun getFetchMethod(): (PublicLinkFolder) -> Flow<List<PublicLinkNode>> =
        { parentFolder ->
            monitorPublicLinkFolderUseCase(parentFolder).map { children ->
                children.map { invoke(addNodeType(it), parentFolder) }
            }
        }
}
