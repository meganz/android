package mega.privacy.android.domain.entity.node

/**
 * Top-down folder path used to rebuild the explorer back stack when jumping straight to a node.
 *
 * @property stack the ordered node ids to push (including the target); empty when it can't be resolved.
 * @property isUnderRootNode whether the target sits under the cloud-drive root (vs. an incoming share).
 */
data class NodeNavigationStack(
    val stack: List<NodeId> = emptyList(),
    val isUnderRootNode: Boolean = false,
)
