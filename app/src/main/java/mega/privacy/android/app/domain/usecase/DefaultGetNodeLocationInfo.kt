package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import javax.inject.Inject

/**
 * Default implementation for [GetNodeLocationInfo]
 */
class DefaultGetNodeLocationInfo @Inject constructor(
    private val megaNodeUtilWrapper: MegaNodeUtilWrapper,
    private val nodeRepository: NodeRepository,
) : GetNodeLocationInfo {
    override suspend fun invoke(typedNode: TypedNode): LocationInfo? {
        val fromIncomingShare = (nodeRepository.getOwnerIdFromInShare(typedNode.id, true) != null)
        return megaNodeUtilWrapper.getNodeLocationInfo(
            -1,
            fromIncomingShare,
            typedNode.id.longValue
        )
    }
}