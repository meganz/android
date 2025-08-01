package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction

internal data class StalledIssueUiItem(
    val syncId: Long,
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeNames: List<String>,
    val displayedName: String,
    val displayedPath: String,
    @DrawableRes val icon: Int,
    val detailedInfo: StalledIssueDetailedInfo,
    val actions: List<StalledIssueResolutionAction>,
)
