package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Returns the remote folder NodeIds of all synced folders.
 * Lightweight alternative to GetFolderPairsUseCase when only IDs are needed.
 */
class GetSyncedNodeIdsUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(): List<NodeId> =
        syncRepository.getSyncedNodeIds()
}
