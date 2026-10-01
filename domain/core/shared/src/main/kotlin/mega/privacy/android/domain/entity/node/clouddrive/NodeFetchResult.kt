package mega.privacy.android.domain.entity.node.clouddrive

import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node fetch result
 *
 * @property loadingState
 * @property hasMediaItems
 * @property typedNodes
 */
data class NodeFetchResult(
    val loadingState: NodesLoadingState,
    val hasMediaItems: Boolean,
    val typedNodes: List<TypedNode>,
)