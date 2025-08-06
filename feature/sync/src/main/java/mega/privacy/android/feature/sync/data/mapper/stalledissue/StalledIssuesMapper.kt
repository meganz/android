package mega.privacy.android.feature.sync.data.mapper.stalledissue

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import nz.mega.sdk.MegaSyncStall
import timber.log.Timber
import javax.inject.Inject

/**
 * Maps a list of [MegaSyncStall] objects to a list of [StalledIssue] domain entities.
 */
internal class StalledIssuesMapper @Inject constructor(
    private val stalledIssueTypeMapper: StalledIssueTypeMapper,
) {
    /**
     * Maps a list of [MegaSyncStall] objects, representing stalled sync issues from the SDK,
     * to a list of [StalledIssue] domain entities.
     *
     * @param syncs The list of current [FolderPair]s representing active sync configurations.
     * @param stalledIssues The list of [MegaSyncStall] objects from the SDK.
     * @return A list of [StalledIssue] domain entities. Returns an empty list if either
     * [syncs] or [stalledIssues] is empty.
     */
    operator fun invoke(
        syncs: List<FolderPair>,
        stalledIssues: List<MegaSyncStall>,
    ): List<StalledIssue> {
        if (syncs.isEmpty() || stalledIssues.isEmpty()) {
            return emptyList()
        }

        return stalledIssues.mapNotNull { stalledIssueSdkObject ->
            try {
                val nodes = getNodes(stalledIssueSdkObject)
                val localPaths = getLocalPaths(stalledIssueSdkObject)

                StalledIssue(
                    syncId = getSyncId(syncs = syncs, nodeInfoList = nodes, localPaths = localPaths)
                        ?: -1L, // Use -1L as Invalid SyncId if no sync matches
                    nodeIds = nodes.map { it.nodeId },
                    localPaths = localPaths,
                    issueType = stalledIssueTypeMapper(stalledIssueSdkObject.reason()),
                    conflictName = stalledIssueSdkObject.reasonDebugString(),
                    nodeNames = nodes.map { it.nodeName },
                    id = stalledIssueSdkObject.hash.toString()
                ).also {
                    Timber.d("Mapped stalled issue: $it and reason: ${stalledIssueSdkObject.reason()}")
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

    private fun getSyncId(
        syncs: List<FolderPair>,
        nodeInfoList: List<NodeInfo>,
        localPaths: List<String>,
    ): Long? =
        syncs.firstOrNull {
            isPartOfSync(
                sync = it,
                nodeInfoList = nodeInfoList,
                localPaths = localPaths
            )
        }?.id

    private fun isPartOfSync(
        sync: FolderPair,
        nodeInfoList: List<NodeInfo>,
        localPaths: List<String>,
    ): Boolean {
        // First try to match by local path
        val hasLocalPathMatch = localPaths.any { localPath ->
            localPath.startsWith(sync.localFolderPath)
        }

        if (hasLocalPathMatch) return true

        // Fallback to remote folder name matching
        return nodeInfoList.any { node ->
            node.nodeName.startsWith(sync.remoteFolder.name)
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

    /**
     * Retrieves the local paths from the stalled issue SDK object.
     *
     * @param stalledIssueSdkObject The [MegaSyncStall] object containing the stalled issue details.
     * @return A list of local paths associated with the stalled issue.
     */
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
