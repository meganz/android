package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject

/**
 * Mapper to Map [mega.privacy.android.domain.entity.node.NodeSourceType] to Constant
 */
class NodeSourceTypeToViewTypeMapper @Inject constructor() {

    /**
     * invoke
     * @param nodeSourceType [mega.privacy.android.domain.entity.node.NodeSourceType]
     * @return Int
     */
    operator fun invoke(nodeSourceType: NodeSourceType) = when (nodeSourceType) {
        NodeSourceType.CLOUD_DRIVE, NodeSourceType.HOME -> NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        NodeSourceType.RUBBISH_BIN -> NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
        NodeSourceType.LINKS -> NodeSourceTypeInt.LINKS_ADAPTER
        NodeSourceType.INCOMING_SHARES -> NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
        NodeSourceType.OUTGOING_SHARES -> NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
        NodeSourceType.BACKUPS -> NodeSourceTypeInt.BACKUPS_ADAPTER
        NodeSourceType.FAVOURITES -> NodeSourceTypeInt.FAVOURITES_ADAPTER
        NodeSourceType.DOCUMENTS -> NodeSourceTypeInt.DOCUMENTS_BROWSE_ADAPTER
        NodeSourceType.AUDIO -> NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER
        NodeSourceType.VIDEOS -> NodeSourceTypeInt.VIDEO_BROWSE_ADAPTER
        NodeSourceType.SEARCH -> NodeSourceTypeInt.SEARCH_BY_ADAPTER
        NodeSourceType.OTHER, NodeSourceType.OFFLINE -> null
    }
}