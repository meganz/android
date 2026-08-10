package mega.privacy.android.core.nodecomponents.model.label

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId

/**
 * State contains label info
 * @property labelList List of [Label]
 * @property nodeId [NodeId] for single node mode
 * @property nodeIds [List] of [NodeId] for multiple nodes mode
 */
data class ChangeLabelState(
    val labelList: List<Label> = emptyList(),
    val nodeId: NodeId? = null,
    val nodeIds: List<NodeId>? = null,
) {
    /**
     * Determines if "Remove label" should be visible
     */
    val isRemoveEnabled = labelList.any { it.isSelected }
}

/**
 * Info about label
 * @property label [NodeLabel]
 * @property labelColor
 * @property labelName
 * @property isSelected
 */
data class Label(
    val label: NodeLabel,
    @ColorRes val labelColor: Int,
    @StringRes val labelName: Int,
    val isSelected: Boolean,
)
