package mega.privacy.android.feature.sync.data.mapper.stalledissue

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import nz.mega.sdk.MegaSyncStall
import nz.mega.sdk.MegaSyncStallList
import javax.inject.Inject

internal class StalledIssuesMapper @Inject constructor(
    private val stalledIssueTypeMapper: StalledIssueTypeMapper,
) {

    operator fun invoke(
        syncs: List<FolderPair>,
        stalledIssues: MegaSyncStallList,
    ): List<StalledIssue> {
        val issuesCount = stalledIssues.size()
        return (0 until issuesCount).map { index ->
            val stalledIssueSdkObject = stalledIssues.get(index)
            val nodes = getNodes(stalledIssueSdkObject)
            val localPaths: List<String> = getLocalPaths(stalledIssueSdkObject)
            StalledIssue(
                syncId = getSyncId(syncs, nodes, localPaths) ?: -1,
                nodeIds = nodes.map { it.nodeId },
                localPaths = localPaths,
                issueType = stalledIssueTypeMapper(stalledIssueSdkObject.reason()),
                conflictName = stalledIssueSdkObject.reasonDebugString(),
                nodeNames = nodes.map { it.nodeName },
            )
        }
    }

    private fun getSyncId(
        syncs: List<FolderPair>,
        megaNodes: List<NodeInfo>,
        localPaths: List<String>,
    ): Long? =
        syncs.firstOrNull { isPartOfSync(it, megaNodes, localPaths) }?.id

    private fun isPartOfSync(
        sync: FolderPair,
        megaNodes: List<NodeInfo>,
        localPaths: List<String>,
    ): Boolean {
        return localPaths.firstOrNull()?.contains(sync.localFolderPath)
            ?: (megaNodes.firstOrNull()?.nodeName?.contains(sync.remoteFolder.name) ?: false)
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