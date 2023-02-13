package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default implementation of [DeleteNodeVersionsByHandle]
 * @param nodeRepository [NodeRepository]
 */
class DefaultDeleteNodeVersionsByHandle @Inject constructor(val nodeRepository: NodeRepository) :
    DeleteNodeVersionsByHandle {
    override suspend fun invoke(nodeToDeleteVersions: NodeId) {
        val versions = nodeRepository.getNodeHistoryVersions(nodeToDeleteVersions)
        val versionsToRemove = versions.drop(1)

        val numErrorsDeleting = versionsToRemove.mapNotNull { versionToRemove ->
            runCatching {
                nodeRepository.deleteNodeVersionByHandle(versionToRemove.id)
            }.exceptionOrNull()
        }.size
        if (numErrorsDeleting > 0) {
            throw VersionsNotDeletedException(
                versionsToRemove.size,
                numErrorsDeleting
            )

        }
    }
}