package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNavigationStack
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import javax.inject.Inject

/**
 * Resolves the top-down folder path to [nodeId] (including it) used to rebuild the explorer back
 * stack when jumping straight to a node — e.g. tapping a deep search result or resuming on the last
 * copy/move target. A node under the cloud-drive root drops the root (the explorer's base); an
 * incoming-share node keeps its full ancestor chain (share root included). Returns an empty path
 * when the node can't be resolved, letting callers apply their own fallback.
 */
class GetNodeNavigationStackUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getAncestorsIdsUseCase: GetAncestorsIdsUseCase,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
) {
    suspend operator fun invoke(nodeId: NodeId): NodeNavigationStack {
        val node = getNodeByIdUseCase(nodeId) ?: return NodeNavigationStack()
        val ancestors = getAncestorsIdsUseCase(node)
        val rootNodeId = runCatching { getRootNodeIdUseCase() }.getOrNull()
        val isUnderRootNode = rootNodeId != null && rootNodeId in ancestors
        val path = if (isUnderRootNode) {
            ancestors.takeWhile { it != rootNodeId }.reversed() + nodeId
        } else {
            ancestors.reversed() + nodeId
        }
        return NodeNavigationStack(stack = path, isUnderRootNode = isUnderRootNode)
    }
}
