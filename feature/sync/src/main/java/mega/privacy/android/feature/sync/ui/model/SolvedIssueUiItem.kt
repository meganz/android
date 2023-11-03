package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.NodeId

internal data class SolvedIssueUiItem(
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val resolutionExplanation: String,
    @DrawableRes val icon: Int,
)
