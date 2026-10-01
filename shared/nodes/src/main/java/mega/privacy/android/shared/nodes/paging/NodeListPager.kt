package mega.privacy.android.shared.nodes.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodePage
import mega.privacy.android.domain.entity.node.PageRequest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Loads a node list page by page from an offset-paged query and keeps the loaded prefix in step
 * with the source: a change signal re-reads the loaded prefix in one call and replaces the items.
 *
 * The pager owns no scope. The caller runs [run] in a job per query (folder, sort, filter) and
 * cancels it when the query changes, so everything happens on one coroutine and no stale result
 * can land.
 *
 * @param pageSize smallest request after the first page
 * @param initialDepth first visible index restored after process death; the first page covers it
 * @param prefetchDistance rows to keep loaded ahead of the last visible row
 * @param maxRequestSize largest request after the first page; a fling is served in one request
 * @param load the paged query
 * @param map page of nodes to list items; runs per page and over the prefix on a re-read
 * @param count folder child count for [PagedNodes.total], null when the query has none
 */
class NodeListPager<T : TypedNode>(
    private val pageSize: Int,
    private val initialDepth: Int = 0,
    private val prefetchDistance: Int = pageSize / 2,
    private val maxRequestSize: Int = pageSize * MAX_REQUEST_PAGES,
    private val load: suspend (PageRequest) -> NodePage<T>,
    private val map: suspend (List<T>) -> List<TypedNodeItem<T>>,
    private val count: (suspend () -> Int)? = null,
) {
    init {
        require(pageSize > 0) { "pageSize must be positive, was $pageSize" }
        require(initialDepth >= 0) { "initialDepth must not be negative, was $initialDepth" }
        require(maxRequestSize >= pageSize) { "maxRequestSize must be at least pageSize" }
    }

    private val _state = MutableStateFlow(PagedNodes<T>())

    /**
     * The loaded prefix and its load state
     */
    val state: StateFlow<PagedNodes<T>> = _state.asStateFlow()

    private val requestedCount = MutableStateFlow(initialDepth + pageSize)
    private val pendingChanges = MutableStateFlow(0)
    private val retryTicks = MutableStateFlow(0)

    private val loadedIds = HashSet<NodeId>()
    private var nextOffset = 0
    private var failedAt: Pair<Int, Int>? = null

    private val loadedCount get() = _state.value.items.size
    private val hasMore get() = _state.value.hasMore

    /**
     * Retry a failed page. Nothing retries by itself: a larger visible range, a change or this does.
     */
    fun retry() {
        retryTicks.update { it + 1 }
    }

    /**
     * Runs the pager until the calling coroutine is cancelled.
     *
     * @param visibleRange item indices currently on screen
     * @param changes signal that the source changed and the loaded prefix must be read again
     * @param publishGate whether a re-read may start; false while scrolling or unobserved. Never
     * holds back loading more.
     */
    @OptIn(FlowPreview::class)
    suspend fun run(
        visibleRange: Flow<IntRange>,
        changes: Flow<Unit>,
        publishGate: StateFlow<Boolean>,
    ): Unit = coroutineScope {
        launch {
            visibleRange.debounce(SETTLE_DELAY).collect { range ->
                requestedCount.update { maxOf(it, range.last + 1 + prefetchDistance) }
            }
        }
        launch {
            changes.collect { pendingChanges.update { it + 1 } }
        }
        while (true) {
            when (awaitWork(publishGate)) {
                Work.LoadPage -> loadPage()
                Work.Reconcile -> reconcile()
                Work.Reload -> reload()
            }
        }
    }

    private suspend fun awaitWork(publishGate: StateFlow<Boolean>): Work = combine(
        requestedCount,
        pendingChanges,
        retryTicks,
        publishGate,
    ) { requested, pending, retry, canPublish ->
        when {
            // Nothing to re-read: load the first page again
            pending > 0 && loadedCount == 0 -> Work.Reload
            pending > 0 && canPublish -> Work.Reconcile
            hasMore && loadedCount < requested && failedAt != (requested to retry) -> Work.LoadPage
            else -> null
        }
    }.filterNotNull().first()

    private fun reload() {
        pendingChanges.value = 0
        nextOffset = 0
        failedAt = null
        _state.update { it.copy(hasMore = true) }
    }

    private suspend fun loadPage() {
        val isFirstPage = loadedCount == 0
        val request = PageRequest(
            offset = nextOffset,
            limit = if (isFirstPage) {
                initialDepth + pageSize
            } else {
                (requestedCount.value - loadedCount).coerceIn(pageSize, maxRequestSize)
            },
        )
        _state.update {
            it.copy(loadState = if (isFirstPage) PagedLoadState.FirstPage else PagedLoadState.LoadingMore)
        }
        val existing = _state.value.items
        val page: NodePage<T>
        val fresh: List<T>
        val items: List<TypedNodeItem<T>>
        try {
            page = load(request)
            fresh = page.nodes.filter { it.id !in loadedIds }
            items = map(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Page $request failed")
            failedAt = requestedCount.value to retryTicks.value
            _state.update {
                it.copy(
                    loadState = if (isFirstPage) PagedLoadState.FirstPageFailed else PagedLoadState.PageFailed,
                )
            }
            return
        }
        val total = if (isFirstPage) readCount() else _state.value.total
        fresh.mapTo(loadedIds) { it.id }
        nextOffset = page.nextOffset
        _state.update {
            it.copy(
                items = existing + items,
                loadState = PagedLoadState.Idle,
                hasMore = page.hasMore,
                total = total,
            )
        }
    }

    private suspend fun reconcile() {
        pendingChanges.value = 0
        val page: NodePage<T>
        val nodes: List<T>
        val items: List<TypedNodeItem<T>>
        try {
            page = load(PageRequest(offset = 0, limit = loadedCount))
            nodes = page.nodes.distinctBy { it.id }
            items = map(nodes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Re-read of the loaded prefix failed, keeping the loaded items")
            return
        }
        val total = readCount()
        loadedIds.clear()
        nodes.mapTo(loadedIds) { it.id }
        nextOffset = page.nextOffset
        failedAt = null
        _state.update {
            it.copy(
                items = items,
                loadState = if (it.loadState == PagedLoadState.PageFailed) PagedLoadState.Idle else it.loadState,
                hasMore = page.hasMore,
                total = total,
                reconcileVersion = it.reconcileVersion + 1,
            )
        }
    }

    private suspend fun readCount(): Int? = count?.let { read ->
        runCatching { read() }
            .onFailure { if (it is CancellationException) throw it }
            .onFailure { Timber.e(it, "Child count failed") }
            .getOrNull()
    }

    private enum class Work { LoadPage, Reconcile, Reload }

    companion object {
        /**
         * Lets a fling settle so it issues one request for its final position
         */
        internal val SETTLE_DELAY: Duration = 150.milliseconds

        /**
         * Default bound on one append request, in pages
         */
        internal const val MAX_REQUEST_PAGES = 4
    }
}
