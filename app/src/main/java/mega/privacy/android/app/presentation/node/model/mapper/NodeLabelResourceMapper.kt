package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.label.Label
import mega.privacy.android.domain.entity.NodeLabel
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
                labelColor = R.color.salmon_400_salmon_300,
                labelName = R.string.label_red,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.ORANGE -> Label(
                label = nodeLabel,
                labelColor = R.color.orange_400_orange_300,
                labelName = R.string.label_orange,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.YELLLOW -> Label(
                label = nodeLabel,
                labelColor = R.color.yellow_600_yellow_300,
                labelName = R.string.label_yellow,
                isSelected = nodeLabel == selectedLabel
            )

            NodeLabel.GREEN -> Label(
                label = nodeLabel,
                labelColor = R.color.green_400_green_300,
                labelName = R.string.label_green,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.BLUE -> Label(
                label = nodeLabel,
                labelColor = R.color.blue_300_blue_200,
                labelName = R.string.label_blue,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.PURPLE -> Label(
                label = nodeLabel,
                labelColor = R.color.purple_300_purple_200,
                labelName = R.string.label_purple,
                isSelected = nodeLabel == selectedLabel
            )


            NodeLabel.GREY -> Label(
                label = nodeLabel,
                labelColor = R.color.grey_300,
                labelName = R.string.label_grey,
                isSelected = nodeLabel == selectedLabel
            )
        }
}