package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.sync.domain.entity.StallIssueType

internal data class StalledIssueUiItem(
    val nodeId: Long,
    val localPath: String,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeName: String,
    @DrawableRes val icon: Int,
    val detailedInfo: StalledIssueDetailedInfo,
    val actions: List<StalledIssueResolutionAction>,
)
