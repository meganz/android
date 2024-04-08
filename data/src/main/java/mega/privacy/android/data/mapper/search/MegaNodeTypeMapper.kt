package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.NodeType
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper used to map the NodeType to MegaNode type
 */
class MegaNodeTypeMapper @Inject constructor() {

    /**
     * invoke
     *
     * @param type NodeType
     */
    operator fun invoke(type: NodeType): Int = when (type) {
        NodeType.FILE -> MegaNode.TYPE_FILE
        NodeType.FOLDER -> MegaNode.TYPE_FOLDER
        NodeType.UNKNOWN -> MegaNode.TYPE_UNKNOWN
    }
}