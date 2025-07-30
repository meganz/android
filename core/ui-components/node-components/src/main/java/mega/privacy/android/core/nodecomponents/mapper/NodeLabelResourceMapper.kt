package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.entity.label.Label
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

/**
 * Mapper to change from megaNode to Color Int
 */
class NodeLabelResourceMapper @Inject constructor() {

    /**
     * Invoke
     * @param nodeLabel
     * @param selectedLabel
     * @return [Label]
     */
    operator fun invoke(nodeLabel: NodeLabel, selectedLabel: NodeLabel?): Label =
        when (nodeLabel) {
            NodeLabel.RED -> Label(
                label = nodeLabel,
                labelColor = R.color.label_red,
                labelName = sharedResR.string.label_red,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.ORANGE -> Label(
                label = nodeLabel,
                labelColor = R.color.label_orange,
                labelName = sharedResR.string.label_orange,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.YELLLOW -> Label(
                label = nodeLabel,
                labelColor = R.color.label_yellow,
                labelName = sharedResR.string.label_yellow,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.GREEN -> Label(
                label = nodeLabel,
                labelColor = R.color.label_green,
                labelName = sharedResR.string.label_green,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.BLUE -> Label(
                label = nodeLabel,
                labelColor = R.color.label_blue,
                labelName = sharedResR.string.label_blue,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.PURPLE -> Label(
                label = nodeLabel,
                labelColor = R.color.label_purple,
                labelName = sharedResR.string.label_purple,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.GREY -> Label(
                label = nodeLabel,
                labelColor = R.color.label_grey,
                labelName = sharedResR.string.label_grey,
                isSelected = nodeLabel == selectedLabel
            )
        }
}