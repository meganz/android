package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.core.R as coreR
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import javax.inject.Inject

internal class StalledIssueItemMapper @Inject constructor(
    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper,
) {

    operator fun invoke(stalledIssueEntity: StalledIssue, areAllNodesFolders: Boolean) =
        StalledIssueUiItem(
            nodeIds = stalledIssueEntity.nodeIds,
            localPaths = stalledIssueEntity.localPaths,
            issueType = stalledIssueEntity.issueType,
            conflictName = stalledIssueEntity.conflictName,
            nodeNames = stalledIssueEntity.nodeNames,
            icon = if (areAllNodesFolders) {
                coreR.drawable.ic_folder_list
            } else {
                iconPackR.drawable.ic_generic_list
            },
            detailedInfo = getMockStalledIssueResolveInfo(),
            actions = stalledIssueResolutionActionMapper(
                stalledIssueEntity.issueType,
                areAllNodesFolders
            )
        )

    operator fun invoke(stalledIssueUiItem: StalledIssueUiItem): StalledIssue =
        StalledIssue(
            nodeIds = stalledIssueUiItem.nodeIds,
            localPaths = stalledIssueUiItem.localPaths,
            issueType = stalledIssueUiItem.issueType,
            conflictName = stalledIssueUiItem.conflictName,
            nodeNames = stalledIssueUiItem.nodeNames,
        )

    private fun getMockStalledIssueResolveInfo(): StalledIssueDetailedInfo =
        StalledIssueDetailedInfo(
            title = R.string.sync_stalled_issue_detail_conflict_title,
            explanation = R.string.sync_stalled_issue_detail_conflict_message
        )
}