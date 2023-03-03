package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsAvailableOffline
import javax.inject.Inject

/**
 * Default implementation for [GetNodeLocationInfo]
 */
class DefaultGetNodeLocationInfo @Inject constructor(
    private val megaNodeUtilWrapper: MegaNodeUtilWrapper,
    private val nodeRepository: NodeRepository,
    private val isAvailableOffline: IsAvailableOffline,
) : GetNodeLocationInfo {
    override suspend fun invoke(typedNode: TypedNode): LocationInfo? {
        val fromIncomingShare = (nodeRepository.getOwnerIdFromInShare(typedNode.id, true) != null)
        val fromOffline = isAvailableOffline(typedNode)
        return megaNodeUtilWrapper.getNodeLocationInfo(
            if (fromOffline) Constants.OFFLINE_ADAPTER else -1,
            fromIncomingShare,
            typedNode.id.longValue
        )
    }
}