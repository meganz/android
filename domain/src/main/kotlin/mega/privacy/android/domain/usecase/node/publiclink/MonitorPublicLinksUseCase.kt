package mega.privacy.android.domain.usecase.node.publiclink

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import javax.inject.Inject

/**
 * Monitor public links use case
 *
 * @property shareRepository
 * @property mapNodeToPublicLinkUseCase
 * @property nodeRepository
 * @constructor Create empty Monitor public links use case
 */
class MonitorPublicLinksUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val nodeRepository: NodeRepository,
) {
    private var nodeIds = emptySet<NodeId>()

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke() = nodeRepository.monitorNodeUpdates()
        .filter { update ->
            isPublicLinkUpdate(update) || update.changes.keys.map { it.id }.intersect(
                nodeIds
            ).isNotEmpty()
        }.map {
            getPublicLinks()
        }.onStart {
            emit(getPublicLinks())
        }

    private fun isPublicLinkUpdate(update: NodeUpdate) =
        update.changes.values.any {
            it.contains(NodeChanges.Public_link)
        }

    private suspend fun getPublicLinks(): List<PublicLinkNode> {
        val publicLinks = shareRepository.getPublicLinks(getLinksSortOrder())
        nodeIds = publicLinks.mapTo(mutableSetOf()) { it.id }
        return publicLinks
            .map { mapNodeToPublicLinkUseCase(it, null) }
    }
}