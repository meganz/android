package mega.privacy.android.app.presentation.node.label

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId

/**
 * State contains label info
 * @property labelList List of [Label]
 * @property nodeId [NodeId]
 */
data class ChangeLabelState(
    val labelList: List<Label> = emptyList(),
    val nodeId: NodeId? = null
)

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
