package mega.privacy.android.app.domain.usecase.offline

import android.app.Activity
import kotlinx.coroutines.rx3.await
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Default implementation of [RemoveAvailableOfflineUseCase]
 */
class DefaultRemoveAvailableOfflineUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getNodeUseCase: GetNodeUseCase,
) : RemoveAvailableOfflineUseCase {
    override suspend fun invoke(
        nodeId: NodeId,
        activity: WeakReference<Activity>,
    ) {
        megaNodeRepository.getNodeByHandle(nodeId.longValue)?.let { megaNode ->
            activity.get()?.let { activity ->
                getNodeUseCase.removeNodeAvailableOffline(
                    node = megaNode,
                    activity = activity
                ).await()
            }
        }
    }
}