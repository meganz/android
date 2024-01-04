package mega.privacy.android.data.mapper.node.label

import mega.privacy.android.domain.entity.NodeLabel
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to convert [NodeLabel] to Int
 */
class NodeLabelIntMapper @Inject constructor() {
    /**
     * Invoke
     * @param nodeLabel [NodeLabel]
     * @return Int
     */
    operator fun invoke(nodeLabel: NodeLabel): Int =
        when (nodeLabel) {
            NodeLabel.RED -> MegaNode.NODE_LBL_RED
            NodeLabel.GREEN -> MegaNode.NODE_LBL_GREEN
            NodeLabel.PURPLE -> MegaNode.NODE_LBL_PURPLE
            NodeLabel.BLUE -> MegaNode.NODE_LBL_BLUE
            NodeLabel.YELLLOW -> MegaNode.NODE_LBL_YELLOW
            NodeLabel.ORANGE -> MegaNode.NODE_LBL_ORANGE
            NodeLabel.GREY -> MegaNode.NODE_LBL_GREY
        }
}