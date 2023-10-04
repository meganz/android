package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.core.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import javax.inject.Inject

internal class StalledIssueItemMapper @Inject constructor() {

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
            }
        )
}