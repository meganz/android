package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to convert [MegaNode] to [NodeInfoForRecentActions]
 */
internal class NodeInfoForRecentActionsMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param megaNode [MegaNode]
     * @param isPendingShare [Boolean]
     * @return [NodeInfoForRecentActions]
     */
    operator fun invoke(
        megaNode: MegaNode,
        isPendingShare: Boolean,
    ) = NodeInfoForRecentActions(
        id = NodeId(megaNode.handle),
        name = megaNode.name,
        parentId = NodeId(megaNode.parentHandle),
        isFolder = megaNode.isFolder,
        isIncomingShare = megaNode.isInShare,
        isOutgoingShare = megaNode.isOutShare,
        isPendingShare = isPendingShare
    )
}
