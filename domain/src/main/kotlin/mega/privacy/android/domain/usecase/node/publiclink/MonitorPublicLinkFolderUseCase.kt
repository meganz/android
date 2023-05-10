package mega.privacy.android.domain.usecase.node.publiclink

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Monitor public link folder use case
 *
 * @property getCloudSortOrder
 * @property mapNodeToPublicLinkUseCase
 * @property nodeRepository
 * @constructor Create empty Monitor public link folder use case
 */
class MonitorPublicLinkFolderUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
) {
    private var nodeIds = emptySet<NodeId>()

    /**
     * Invoke
     *
     * @param parentFolder
     */
    operator fun invoke(parentFolder: FolderNode) = nodeRepository.monitorNodeUpdates()
        .filter { update ->
            isPublicLinkUpdate(update) || update.changes.keys.map { it.id }.intersect(
                nodeIds
            ).isNotEmpty()
        }.map {
            getPublicLinks(parentFolder)
        }.onStart {
            emit(getPublicLinks(parentFolder))
        }

    private fun isPublicLinkUpdate(update: NodeUpdate) =
        update.changes.values.any {
            it.contains(NodeChanges.Public_link)
        }

    private suspend fun getPublicLinks(parentFolder: FolderNode): List<UnTypedNode> {
        val publicLinks = parentFolder.fetchChildren(getCloudSortOrder())
        nodeIds = publicLinks.mapTo(mutableSetOf()) { it.id }
        return publicLinks
    }
}