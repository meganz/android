package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffResult
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

/**
 * Monitors recently used items, excluding nodes that can no longer be resumed by the current user.
 *
 * Two kinds of items are dropped from the carousel:
 *
 * - **Moved to the rubbish bin or deleted.** These are never resumable, so they are removed
 *   unconditionally ([IsNodeInRubbishOrDeletedUseCase]) regardless of any hidden-nodes setting.
 * - **Hidden from the current user.** A node's raw sensitivity flag is not enough to decide
 *   visibility: the hidden-nodes feature is paid-only, so a sensitive node must stay visible for
 *   free users (and for anyone who has turned "show hidden items" on). Hidden items are therefore
 *   only removed when the feature is actually active for this account
 *   ([MonitorHiddenNodesEnabledUseCase]) AND the user is not showing hidden items
 *   ([MonitorShowHiddenItemsUseCase]). When the feature is active and the user IS showing hidden
 *   items, the hidden items are kept but flagged [ContinueWhereLeftOffItem.isSensitive] so the
 *   carousel can blur them, mirroring how the node lists mark sensitive content.
 *
 * In both cases the item is removed from the index so it does not reappear once dropped.
 *
 * Surviving items also have their title refreshed from the current node name: the
 * recently-used table only stores the file name captured when the item was opened, so a
 * later rename would otherwise leave a stale title in the carousel. Like the removals above,
 * this is re-evaluated against the live node on every relevant change (and on each new
 * subscription), so the title self-heals even when the rename happened off-screen.
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
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
) {
    operator fun invoke(
        limit: Int,
        sortField: ContinueWhereLeftOffSortField? = null,
        sortDirection: SortDirection? = null,
    ): Flow<ContinueWhereLeftOffResult> {
        val itemsFlow = repository.monitorContinueWhereLeftOffItems(limit, sortField, sortDirection)
        return combine(
            itemsFlow,
            // Emit a null placeholder until the real eligibility loads so the list is never blocked
            // waiting for the account, while still telling consumers (via isHiddenResolved) that the
            // sensitivity is not yet known. A null placeholder is treated as "feature disabled" here
            // so nothing is removed and nothing is blurred before the real value arrives — consumers
            // keep showing their loading state instead of rendering these unresolved items.
            monitorHiddenNodesEnabledUseCase().map<Boolean, Boolean?> { it }.onStart { emit(null) },
            monitorShowHiddenItemsUseCase().onStart { emit(true) },
            relevantNodeUpdates(itemsFlow),
        ) { items, hiddenNodesEnabledOrNull, showHiddenItems, _ ->
            buildResult(items, hiddenNodesEnabledOrNull, showHiddenItems)
        }.distinctUntilChanged()
    }

    /**
     * Applies the current hidden-nodes settings to [items] and refreshes each surviving item from
     * its live node. Trashed/deleted items are always dropped; hidden items are dropped or flagged
     * depending on the feature state and the "show hidden items" setting.
     */
    private suspend fun buildResult(
        items: List<ContinueWhereLeftOffItem>,
        hiddenNodesEnabledOrNull: Boolean?,
        showHiddenItems: Boolean,
    ): ContinueWhereLeftOffResult {
        val isHiddenResolved = hiddenNodesEnabledOrNull != null
        val hiddenNodesEnabled = hiddenNodesEnabledOrNull == true

        // Items moved to the rubbish bin or deleted are never resumable, so drop them regardless of
        // the hidden-nodes feature or the "show hidden items" setting.
        val trashedHandles = items.trashedNodeHandles()
        trashedHandles.forEach { repository.removeRecentlyUsedItem(it) }
        val resumableItems = items.filterNot { it.nodeHandle in trashedHandles }

        val visibleItems = when {
            // Eligibility not resolved yet, or feature off for this account: nothing is
            // sensitive, show as-is. Nothing is removed while unresolved.
            !hiddenNodesEnabled -> resumableItems
            // Feature on and showing hidden items: keep the hidden ones but flag them so the
            // carousel blurs them, the same way the node lists render sensitive content.
            showHiddenItems -> {
                val hiddenHandles = resumableItems.hiddenNodeHandles()
                resumableItems.map { it.copy(isSensitive = it.nodeHandle in hiddenHandles) }
            }
            // Feature on and not showing hidden items: drop the hidden ones entirely.
            else -> {
                val hiddenHandles = resumableItems.hiddenNodeHandles()
                hiddenHandles.forEach { repository.removeRecentlyUsedItem(it) }
                resumableItems.filterNot { it.nodeHandle in hiddenHandles }
            }
        }
        return ContinueWhereLeftOffResult(
            items = visibleItems.withRefreshedNodeInfo(),
            isHiddenResolved = isHiddenResolved,
        )
    }

    /**
     * Ticks whenever a node update touches one of the current carousel items. The node-update
     * stream is global — it fires for every node in the app — so it is scoped to the handles of the
     * latest [itemsFlow] emission and matched on node identity rather than change type: takedown or
     * restore has no dedicated SDK flag (it arrives as a public-link change), so filtering by
     * *which* node changed — not *what* changed — keeps the taken-down icon in sync without reacting
     * to unrelated changes elsewhere. The initial tick lets the combine produce a first value before
     * any node change; debounce coalesces bursts.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun relevantNodeUpdates(itemsFlow: Flow<List<ContinueWhereLeftOffItem>>): Flow<NodeUpdate> =
        itemsFlow
            .flatMapLatest { items ->
                val handles = items.mapTo(mutableSetOf()) { it.nodeHandle }
                monitorNodeUpdatesUseCase()
                    .filter { update -> update.changes.keys.any { it.id.longValue in handles } }
                    .debounce(NODE_UPDATE_DEBOUNCE_MILLIS)
            }
            .onStart { emit(NodeUpdate(emptyMap())) }

    /**
     * Refreshes each item from its current node in a single lookup:
     *
     * - **Title:** the recently-used table only stores the name captured when the item was opened,
     *   so a later rename would otherwise leave a stale title. A lookup failure or a blank name
     *   keeps the stored title, so a transient error never blanks a title.
     * - **Taken-down:** the table has no taken-down flag, so it is resolved from the live node here
     *   ([ContinueWhereLeftOffItem.isTakenDown]); the carousel and list use it to show the generic
     *   file-type icon instead of the original thumbnail. A lookup failure keeps the stored flag.
     */
    private suspend fun List<ContinueWhereLeftOffItem>.withRefreshedNodeInfo(): List<ContinueWhereLeftOffItem> =
        coroutineScope {
            map { item ->
                async {
                    val node = runCatching { getNodeByIdUseCase(NodeId(item.nodeHandle)) }.getOrNull()
                    val currentName = node?.name?.takeIf { it.isNotBlank() }
                    item.copy(
                        title = currentName ?: item.title,
                        isTakenDown = node?.isTakenDown ?: item.isTakenDown,
                    )
                }
            }.awaitAll()
        }

    private suspend fun List<ContinueWhereLeftOffItem>.trashedNodeHandles(): Set<Long> =
        coroutineScope {
            map { item -> async { item.nodeHandle to isNodeTrashed(item.nodeHandle) } }
                .awaitAll()
                .filter { (_, trashed) -> trashed }
                .map { (nodeHandle, _) -> nodeHandle }
                .toSet()
        }

    /**
     * Whether the node has been moved to the rubbish bin or deleted. A lookup failure is treated as
     * "not trashed" so a transient error never drops a recently-used item here.
     */
    private suspend fun isNodeTrashed(nodeHandle: Long): Boolean =
        runCatching { isNodeInRubbishOrDeletedUseCase(nodeHandle) }.getOrDefault(false)

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

    private companion object {
        /**
         * Coalesces bursts of node updates (e.g. during a sync) so the per-item node lookups run
         * once per quiet window rather than per raw update. Mirrors the debounce the Recents widget
         * applies to the same node-update stream.
         */
        private const val NODE_UPDATE_DEBOUNCE_MILLIS = 500L
    }
}
