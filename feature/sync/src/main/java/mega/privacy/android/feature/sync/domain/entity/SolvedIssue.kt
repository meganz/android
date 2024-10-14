package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Entity representing a solved stalled issue
 * @property syncId - id of the sync
 * @property nodeIds - list of node ids
 * @property localPaths - list of local paths
 * @property resolutionExplanation - explanation of the resolution
 */
data class SolvedIssue(
    val syncId: Long,
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val resolutionExplanation: String,
)
