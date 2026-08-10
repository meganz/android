package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Resolves the current (latest) version of a file node by [nodeId].
 *
 * In MEGA, overwriting a file — for example saving an edited text file back to the cloud —
 * uploads a new node that becomes the current version, while the previous node is kept as a
 * version. A handle captured before the overwrite (such as one persisted in the "Continue Where
 * Left Off" list) therefore points at a stale version, and opening it would show the pre-edit
 * content.
 *
 * Resolution is done in two steps so it is robust to how versions are linked:
 * 1. Walk up the version chain — a previous version's parent is the next-newer version (a file),
 *    while the current version's parent is the containing folder — until a folder is reached.
 * 2. Look up the folder's current child with the same name; that is the up-to-date node.
 *
 * For a node that has not been superseded both steps resolve back to the node itself.
 *
 * @return the current version [TypedFileNode], or null if [nodeId] does not resolve to a file.
 */
class GetCurrentVersionNodeUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
) {
    suspend operator fun invoke(nodeId: NodeId): TypedFileNode? {
        var node = getNodeByIdUseCase(nodeId) as? TypedFileNode ?: return null
        // Climb the version chain until the parent is a folder (i.e. the chain head).
        var depth = 0
        while (depth++ < MAX_VERSION_CHAIN_DEPTH) {
            val parent = getNodeByIdUseCase(node.parentId) as? TypedFileNode ?: break
            node = parent
        }
        // The folder's current child with the same name is the up-to-date node.
        val currentChildId = getChildNodeUseCase(node.parentId, node.name)?.id ?: return node
        return getNodeByIdUseCase(currentChildId) as? TypedFileNode ?: node
    }

    private companion object {
        /** Safety cap so a malformed (cyclic) version chain cannot loop indefinitely. */
        const val MAX_VERSION_CHAIN_DEPTH = 100
    }
}
