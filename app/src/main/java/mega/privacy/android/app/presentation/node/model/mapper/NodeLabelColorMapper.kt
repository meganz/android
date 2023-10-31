package mega.privacy.android.app.presentation.node.model.mapper

import androidx.annotation.ColorRes
import mega.privacy.android.app.R
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to change from megaNode to Color Int
 */
class NodeLabelColorMapper @Inject constructor() {

    /**
     * Invoke
     * @param nodeLabel
     */
    @ColorRes
    operator fun invoke(nodeLabel: Int): Int = when (nodeLabel) {
        MegaNode.NODE_LBL_RED -> R.color.salmon_400_salmon_300
        MegaNode.NODE_LBL_ORANGE -> R.color.orange_400_orange_300
        MegaNode.NODE_LBL_YELLOW -> R.color.yellow_600_yellow_300
        MegaNode.NODE_LBL_GREEN -> R.color.green_400_green_300
        MegaNode.NODE_LBL_BLUE -> R.color.blue_300_blue_200
        MegaNode.NODE_LBL_PURPLE -> R.color.purple_300_purple_200
        else -> R.color.grey_300
    }
}