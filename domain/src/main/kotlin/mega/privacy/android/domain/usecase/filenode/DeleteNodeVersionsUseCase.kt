package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to delete a MegaNode's history versions, referenced by its handle [NodeId]
 */
class DeleteNodeVersionsUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Deletes a MegaNode's history versions referenced by its handle [NodeId]
     * Only last version will be keep
     * @param nodeToDeleteVersions [NodeId] handle of the node whose history we want to delete
     */
    suspend operator fun invoke(nodeToDeleteVersions: NodeId) {
        val versions = nodeRepository.getNodeHistoryVersions(nodeToDeleteVersions)
        val versionsToRemove = versions.drop(1)

        val numErrorsDeleting = versionsToRemove.mapAsync { versionToRemove ->
            runCatching {
                nodeRepository.deleteNodeVersionByHandle(versionToRemove.id)
            }
        }.count { it.isFailure }

        if (numErrorsDeleting > 0) {
            throw VersionsNotDeletedException(
                versionsToRemove.size,
                numErrorsDeleting
            )
        }
    }
}