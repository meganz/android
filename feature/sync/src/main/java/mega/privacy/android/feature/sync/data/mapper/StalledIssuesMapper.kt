package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mock.MegaSyncStallList
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import javax.inject.Inject

internal class StalledIssuesMapper @Inject constructor(
    private val stalledIssueTypeMapper: StalledIssueTypeMapper,
) {

    operator fun invoke(stalledIssues: MegaSyncStallList): List<StalledIssue> {
        val issuesCount = stalledIssues.size()
        return (0 until issuesCount).map { index ->
            val stalledIssueSdkObject = stalledIssues.get(index)
            StalledIssue(
                nodeId = NodeId(stalledIssueSdkObject.cloudNodeHandle(0)),
                localPath = stalledIssueSdkObject.path(false, 0),
                issueType = stalledIssueTypeMapper(stalledIssueSdkObject.reason()),
                conflictName = stalledIssueSdkObject.reasonDebugString(),
                nodeName = stalledIssueSdkObject.path(true, 0),
            )
        }
    }
}