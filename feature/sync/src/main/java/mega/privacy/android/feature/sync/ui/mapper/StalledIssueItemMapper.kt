package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.core.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import javax.inject.Inject

internal class StalledIssueItemMapper @Inject constructor(
    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper
) {

    operator fun invoke(stalledIssueEntity: StalledIssue, isFolder: Boolean) =
        StalledIssueUiItem(
            nodeId = stalledIssueEntity.nodeId.longValue,
            localPath = stalledIssueEntity.localPath,
            issueType = stalledIssueEntity.issueType,
            conflictName = stalledIssueEntity.conflictName,
            nodeName = stalledIssueEntity.nodeName,
            icon = if (isFolder) {
                R.drawable.ic_folder_list
            } else {
                R.drawable.ic_generic_list
            },
            detailedInfo = getMockStalledIssueResolveInfo(),
            actions = stalledIssueResolutionActionMapper(stalledIssueEntity.issueType)
        )

    private fun getMockStalledIssueResolveInfo(): StalledIssueDetailedInfo =
        StalledIssueDetailedInfo(
            "Conflict A", "This folders contain multiple names " +
                    "on one side, that would all become the same single name on the other side. This may" +
                    " be due to syncing to case sensitive local filesystem, or the effects os " +
                    "escaped characters."
        )
}