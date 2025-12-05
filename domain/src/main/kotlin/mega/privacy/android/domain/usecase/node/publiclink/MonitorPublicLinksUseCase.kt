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
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import javax.inject.Inject

/**
 * Monitor public links use case
 *
 * @property shareRepository
 * @property mapNodeToPublicLinkUseCase
 * @property nodeRepository
 * @constructor Create empty Monitor public links use case
 */
@Deprecated(
    message = "This use case is deprecated. Please use MonitorLinksUseCase instead",
    replaceWith = ReplaceWith("MonitorLinksUseCase"),
    level = DeprecationLevel.WARNING,
)
class MonitorPublicLinksUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
    private val getLinksSortOrderUseCase: GetLinksSortOrderUseCase,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val nodeRepository: NodeRepository,
) {
    private var nodeIds = emptySet<NodeId>()

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(isSingleActivityEnabled: Boolean) =
        nodeRepository.monitorNodeUpdates()
            .filter { update ->
                isPublicLinkUpdate(update) || update.changes.keys.map { it.id }.intersect(
                    nodeIds
                ).isNotEmpty()
            }.map {
                getPublicLinks(isSingleActivityEnabled)
            }.onStart {
                emit(getPublicLinks(isSingleActivityEnabled))
            }

    private fun isPublicLinkUpdate(update: NodeUpdate) =
        update.changes.values.any {
            it.contains(NodeChanges.Public_link)
        }

    private suspend fun getPublicLinks(isSingleActivityEnabled: Boolean): List<PublicLinkNode> {
        val publicLinks =
            shareRepository.getPublicLinks(getLinksSortOrderUseCase(isSingleActivityEnabled))
        nodeIds = publicLinks.mapTo(mutableSetOf()) { it.id }
        return publicLinks
            .mapNotNull {
                runCatching {
                    mapNodeToPublicLinkUseCase(it, null)
                }.getOrNull()
            }
    }
}