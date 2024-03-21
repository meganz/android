package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class TryNodeSyncUseCase @Inject constructor(private val syncRepository: SyncRepository) {

    /**
     * Invoke.
     *
     * @param nodeId    Node ID
     * @return  True if the node is syncable, false otherwise
     */
    suspend operator fun invoke(nodeId: NodeId) {
        syncRepository.tryNodeSync(nodeId)
    }
}