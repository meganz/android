package mega.privacy.android.app.domain.usecase.offline

import android.app.Activity
import kotlinx.coroutines.rx3.await
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Default implementation of [SetNodeAvailableOffline]
 */
class DefaultSetNodeAvailableOffline @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getNodeUseCase: GetNodeUseCase,
) : SetNodeAvailableOffline {
    override suspend fun invoke(
        nodeId: NodeId,
        availableOffline: Boolean,
        activity: WeakReference<Activity>,
    ) {
        megaNodeRepository.getNodeByHandle(nodeId.longValue)?.let { megaNode ->
            val fromIncomingShare = megaNodeRepository.getUserFromInShare(megaNode, true) != null
            val fromInbox = megaNodeRepository.isNodeInInbox(megaNode)
            activity.get()?.let { activity ->
                getNodeUseCase.setNodeAvailableOffline(
                    node = megaNode,
                    setOffline = availableOffline,
                    isFromIncomingShares = fromIncomingShare,
                    isFromInbox = fromInbox,
                    activity = activity
                ).await()
            }
        }
    }
}