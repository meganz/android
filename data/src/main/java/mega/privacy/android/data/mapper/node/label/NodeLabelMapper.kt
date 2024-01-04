package mega.privacy.android.data.mapper.node.label

import mega.privacy.android.domain.entity.NodeLabel
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to convert int to [NodeLabel]
 */
class NodeLabelMapper @Inject constructor() {
    /**
     * invoke
     * @param label
     * @return [NodeLabel]
     */
    operator fun invoke(label: Int): NodeLabel? = when (label) {
        MegaNode.NODE_LBL_RED -> NodeLabel.RED
        MegaNode.NODE_LBL_ORANGE -> NodeLabel.ORANGE
        MegaNode.NODE_LBL_YELLOW -> NodeLabel.YELLLOW
        MegaNode.NODE_LBL_GREEN -> NodeLabel.GREEN
        MegaNode.NODE_LBL_PURPLE -> NodeLabel.PURPLE
        MegaNode.NODE_LBL_BLUE -> NodeLabel.BLUE
        MegaNode.NODE_LBL_GREY -> NodeLabel.GREY
        else -> null
    }
}