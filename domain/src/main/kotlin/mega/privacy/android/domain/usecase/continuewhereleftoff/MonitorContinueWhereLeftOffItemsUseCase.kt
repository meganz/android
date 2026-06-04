package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

/**
 * Monitors recently used items, excluding nodes that are hidden from the current user.
 *
 * A node's raw sensitivity flag is not enough to decide visibility: the hidden-nodes feature is
 * paid-only, so a sensitive node must stay visible for free users (and for anyone who has turned
 * "show hidden items" on). Items are therefore only removed when the feature is actually active
 * for this account ([MonitorHiddenNodesEnabledUseCase]) AND the user is not showing hidden items
 * ([MonitorShowHiddenItemsUseCase]). Hidden items are removed from the index so they do not
 * reappear once dropped.
 *
 * When both [sortField] and [sortDirection] are non-null, those explicit values are
 * used and the persisted preference is ignored. Otherwise (either or both null) items
 * follow the persisted sort preference.
 */
class MonitorContinueWhereLeftOffItemsUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    operator fun invoke(
        limit: Int,
        sortField: ContinueWhereLeftOffSortField? = null,
        sortDirection: SortDirection? = null,
    ): Flow<List<ContinueWhereLeftOffItem>> =
        combine(
            repository.monitorContinueWhereLeftOffItems(limit, sortField, sortDirection),
            // Fail-open defaults so the list is never blocked waiting for account/settings to load
            // and nothing is removed before the real values are known: "feature disabled" and
            // "showing hidden items" both mean keep everything. Removal only happens once the real
            // values say the feature is enabled AND hidden items are not being shown.
            monitorHiddenNodesEnabledUseCase().onStart { emit(false) },
            monitorShowHiddenItemsUseCase().onStart { emit(true) },
            // Re-evaluate when a node's sensitivity changes (e.g. the user hides the open file),
            // since that does not alter the recently-used table on its own.
            sensitiveNodeChanges(),
        ) { items, hiddenNodesEnabled, showHiddenItems, _ ->
            if (!hiddenNodesEnabled || showHiddenItems) {
                items
            } else {
                val hiddenHandles = items.hiddenNodeHandles()
                hiddenHandles.forEach { repository.removeRecentlyUsedItem(it) }
                items.filterNot { it.nodeHandle in hiddenHandles }
            }
        }.distinctUntilChanged()

    private fun sensitiveNodeChanges(): Flow<NodeUpdate> =
        monitorNodeUpdatesUseCase()
            .filter { update -> update.changes.values.any { NodeChanges.Sensitive in it } }
            .onStart { emit(NodeUpdate(emptyMap())) }

    private suspend fun List<ContinueWhereLeftOffItem>.hiddenNodeHandles(): Set<Long> =
        coroutineScope {
            map { item -> async { item.nodeHandle to isNodeHidden(item.nodeHandle) } }
                .awaitAll()
                .filter { (_, hidden) -> hidden }
                .map { (nodeHandle, _) -> nodeHandle }
                .toSet()
        }

    /**
     * Whether the node is hidden, either directly (`isMarkedSensitive`) or through a hidden
     * ancestor (`isSensitiveInherited`). Returns false when the node can't be resolved, so a
     * transient lookup failure (or an already-deleted node) never drops a recently-used item here.
     */
    private suspend fun isNodeHidden(nodeHandle: Long): Boolean {
        val node = runCatching { getNodeByIdUseCase(NodeId(nodeHandle)) }.getOrNull()
        return node != null && (node.isMarkedSensitive || node.isSensitiveInherited)
    }
}
