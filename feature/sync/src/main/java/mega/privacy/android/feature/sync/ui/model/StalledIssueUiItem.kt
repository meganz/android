package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo

internal data class StalledIssueUiItem(
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeNames: List<String>,
    @DrawableRes val icon: Int,
    val detailedInfo: StalledIssueDetailedInfo,
    val actions: List<StalledIssueResolutionAction>,
)
