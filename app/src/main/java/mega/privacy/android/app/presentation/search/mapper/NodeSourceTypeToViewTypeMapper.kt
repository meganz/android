package mega.privacy.android.app.presentation.search.mapper

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject

/**
 * Mapper to Map [NodeSourceType] to Constant
 */
class NodeSourceTypeToViewTypeMapper @Inject constructor() {

    /**
     * invoke
     * @param nodeSourceType [NodeSourceType]
     * @return Int
     */
    operator fun invoke(nodeSourceType: NodeSourceType) = when (nodeSourceType) {
        NodeSourceType.CLOUD_DRIVE, NodeSourceType.HOME -> Constants.FILE_BROWSER_ADAPTER
        NodeSourceType.RUBBISH_BIN -> Constants.RUBBISH_BIN_ADAPTER
        NodeSourceType.LINKS -> Constants.LINKS_ADAPTER
        NodeSourceType.INCOMING_SHARES -> Constants.INCOMING_SHARES_ADAPTER
        NodeSourceType.OUTGOING_SHARES -> Constants.OUTGOING_SHARES_ADAPTER
        NodeSourceType.BACKUPS -> Constants.BACKUPS_ADAPTER
        NodeSourceType.OTHER -> null
    }
}