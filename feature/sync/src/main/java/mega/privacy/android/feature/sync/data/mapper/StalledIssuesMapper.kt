package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import nz.mega.sdk.MegaSyncStallList
import nz.mega.sdk.MegaSyncStall
import javax.inject.Inject

internal class StalledIssuesMapper @Inject constructor(
    private val stalledIssueTypeMapper: StalledIssueTypeMapper,
) {

    operator fun invoke(stalledIssues: MegaSyncStallList): List<StalledIssue> {
        val issuesCount = stalledIssues.size()
        return (0 until issuesCount).map { index ->
            val stalledIssueSdkObject = stalledIssues.get(index)
            val nodes = getNodes(stalledIssueSdkObject)
            val localPaths: List<String> = getLocalPaths(stalledIssueSdkObject)
            StalledIssue(
                nodeIds = nodes.map { it.nodeId },
                localPaths = localPaths,
                issueType = stalledIssueTypeMapper(stalledIssueSdkObject.reason()),
                conflictName = stalledIssueSdkObject.reasonDebugString(),
                nodeNames = nodes.map { it.nodeName },
            )
        }
    }

    private fun getNodes(stalledIssueSdkObject: MegaSyncStall): List<NodeInfo> {
        val nodesCount = stalledIssueSdkObject.pathCount(true).toInt()
        return (0 until nodesCount).map { index ->
            val megaNodeHandle = stalledIssueSdkObject.cloudNodeHandle(index)
            val megaNodeName = stalledIssueSdkObject.path(true, index)
            NodeInfo(
                nodeId = NodeId(megaNodeHandle),
                nodeName = megaNodeName
            )
        }
    }

    private fun getLocalPaths(stalledIssueSdkObject: MegaSyncStall): List<String> {
        val nodesCount = stalledIssueSdkObject.pathCount(false).toInt()
        return (0 until nodesCount).map { index ->
            stalledIssueSdkObject.path(false, index)
        }
    }

    private data class NodeInfo(
        val nodeId: NodeId,
        val nodeName: String,
    )
}