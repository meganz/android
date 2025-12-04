package mega.privacy.android.domain.usecase.node.publiclink

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor links use case
 *
 * @property shareRepository
 * @property mapNodeToPublicLinkUseCase
 * @property nodeRepository
 * @property monitorOfflineNodeUpdatesUseCase
 * @constructor Create empty Monitor links use case
 */
class MonitorLinksUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
    private val getLinksSortOrderUseCase: GetLinksSortOrderUseCase,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val nodeRepository: NodeRepository,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
) {
    private var nodeIds = emptySet<NodeId>()

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(isSingleActivityEnabled: Boolean) =
        merge(
            monitorOnlineNodeUpdates(),
            monitorOfflineNodeUpdates()
        )
            .map {
                getPublicLinks(isSingleActivityEnabled)
            }
            .onStart {
                emit(getPublicLinks(isSingleActivityEnabled))
            }

    private fun monitorOnlineNodeUpdates() =
        nodeRepository.monitorNodeUpdates()
            .filter { update ->
                isPublicLinkUpdate(update) || update.changes.keys.map { it.id }.intersect(
                    nodeIds
                ).isNotEmpty()
            }

    private fun monitorOfflineNodeUpdates() =
        monitorOfflineNodeUpdatesUseCase()
            .drop(1) // Skip the first emission (initial load from database)
            .scan(emptySet<Long>()) { previousOfflineHandles, offlineList ->
                // Convert nodeIds to Set<Long> for O(1) lookup performance
                val nodeIdLongValues = nodeIds.mapTo(mutableSetOf()) { it.longValue }

                // Extract handles of offline nodes that match public link node IDs
                offlineList
                    .mapNotNull { offline ->
                        offline.handle.toLongOrNull()?.takeIf { handle ->
                            handle in nodeIdLongValues
                        }
                    }
                    .toSet()
            }
            .distinctUntilChanged() // Only emit when the set of matching offline handles changes
            .drop(1) // Skip the first scan emission (empty set)
            .filter { nodeIds.isNotEmpty() } // Only emit if we're tracking any public link nodes

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

