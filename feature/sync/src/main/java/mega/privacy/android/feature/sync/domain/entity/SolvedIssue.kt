package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

internal data class SolvedIssue(
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val resolutionExplanation: String,
)
