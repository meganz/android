package mega.privacy.android.shared.nodes.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodePage
import mega.privacy.android.domain.entity.node.PageRequest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeListPagerTest {

    private val visibleRange = MutableSharedFlow<IntRange>(extraBufferCapacity = 8)
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val publishGate = MutableStateFlow(true)
    private val requests = mutableListOf<PageRequest>()

    private var source: List<TypedNode> = emptyList()
    private var failNextLoad = false
    private var holdNextLoad: CompletableDeferred<Unit>? = null

    private val load: suspend (PageRequest) -> NodePage<TypedNode> = { request ->
        requests += request
        holdNextLoad?.let { hold ->
            holdNextLoad = null
            hold.await()
        }
        if (failNextLoad) {
            failNextLoad = false
            throw IllegalStateException("load failed")
        }
        val rows = source.drop(request.offset).take(request.limit)
        NodePage(
            nodes = rows,
            nextOffset = request.offset + rows.size,
            hasMore = source.size > request.offset + request.limit,
        )
    }

    private val map: suspend (List<TypedNode>) -> List<TypedNodeItem<TypedNode>> = { nodes ->
        nodes.map { NodeViewItem(node = it) }
    }

    @BeforeEach
    fun reset() {
        requests.clear()
        source = nodes(1L..25L)
        failNextLoad = false
        holdNextLoad = null
        publishGate.value = true
    }

    private fun node(id: Long): TypedNode = mock<TypedFileNode> { on { this.id } doReturn NodeId(id) }

    private fun nodes(ids: LongRange): List<TypedNode> = ids.map { node(it) }

    private fun TestScope.newPager(
        pageSize: Int = 10,
        initialDepth: Int = 0,
        withCount: Boolean = false,
    ) = NodeListPager(
        pageSize = pageSize,
        initialDepth = initialDepth,
        load = load,
        map = map,
        count = if (withCount) ({ source.size }) else null,
    )

    /**
     * Runs the pager in the test scope itself. Work launched in [TestScope.backgroundScope] is
     * skipped by advanceUntilIdle once no foreground task is left, which would leave every page
     * after the first unloaded.
     */
    private fun pagerTest(
        pageSize: Int = 10,
        initialDepth: Int = 0,
        withCount: Boolean = false,
        block: suspend TestScope.(NodeListPager<TypedNode>) -> Unit,
    ) = runTest {
        val pager = newPager(pageSize, initialDepth, withCount)
        val job = launch { pager.run(visibleRange, changes, publishGate) }
        runCurrent()
        block(pager)
        job.cancel()
    }

    private fun NodeListPager<TypedNode>.loadedIds() = state.value.items.map { it.id.longValue }

    private fun reReads() = requests.drop(1).filter { it.offset == 0 }

    /** Lets the debounced visible range through without reaching the retry delay */
    private fun TestScope.settle() {
        advanceTimeBy(NodeListPager.SETTLE_DELAY.inWholeMilliseconds + 1)
        runCurrent()
    }

    @Test
    fun `test that run loads the first page at offset 0 with pageSize rows`() = pagerTest { pager ->
        assertThat(requests).containsExactly(PageRequest(offset = 0, limit = 10))
        assertThat(pager.loadedIds()).isEqualTo((1L..10L).toList())
        assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.Idle)
        assertThat(pager.state.value.hasMore).isTrue()
    }

    @Test
    fun `test that run widens the first page to cover initialDepth in one call`() =
        pagerTest(initialDepth = 15) { pager ->
            assertThat(requests).containsExactly(PageRequest(offset = 0, limit = 25))
            assertThat(pager.loadedIds()).hasSize(25)
        }

    @Test
    fun `test that a visible range near the end loads the next page once when reported twice`() =
        pagerTest { pager ->
            visibleRange.emit(5..9)
            visibleRange.emit(6..9)
            advanceUntilIdle()

            assertThat(requests).containsExactly(
                PageRequest(offset = 0, limit = 10),
                PageRequest(offset = 10, limit = 10),
            )
            assertThat(pager.loadedIds()).isEqualTo((1L..20L).toList())
        }

    @Test
    fun `test that a visible range far from the end loads nothing`() = pagerTest { _ ->
        visibleRange.emit(0..3)
        advanceUntilIdle()

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `test that a fling over several pages is served by one request up to maxRequestSize`() {
        source = nodes(1L..100L)
        pagerTest { pager ->
            visibleRange.emit(0..45)
            advanceUntilIdle()

            // 51 rows requested: 41 missing, capped at 4 pages, then the remaining page
            assertThat(requests.map { it.limit }).containsExactly(10, 40, 10).inOrder()
            assertThat(requests.map { it.offset }).containsExactly(0, 10, 50).inOrder()
            assertThat(pager.loadedIds()).isEqualTo((1L..60L).toList())
        }
    }

    @Test
    fun `test that appended rows are de-duplicated by id and nextOffset follows the page`() =
        pagerTest { pager ->
            // A node inserted above the loaded rows shifts the SDK's offsets by one
            source = listOf(node(100L)) + source

            visibleRange.emit(5..9)
            advanceUntilIdle()

            // The SDK's rows 10..19 are ids 10..19; id 10 is already loaded
            assertThat(pager.loadedIds()).isEqualTo((1L..19L).toList())

            visibleRange.emit(15..18)
            advanceUntilIdle()

            assertThat(pager.loadedIds()).isEqualTo((1L..25L).toList())
            assertThat(requests.map { it.offset }).containsExactly(0, 10, 20).inOrder()
        }

    @Test
    fun `test that no page is loaded once hasMore is false`() {
        source = nodes(1L..10L)
        pagerTest { pager ->
            visibleRange.emit(5..9)
            advanceUntilIdle()

            assertThat(requests).hasSize(1)
            assertThat(pager.state.value.hasMore).isFalse()
        }
    }

    @Test
    fun `test that a change re-reads the loaded prefix in one call and replaces the items`() =
        pagerTest { pager ->
            visibleRange.emit(5..9)
            advanceUntilIdle()
            // Node 3 deleted, node 5 renamed so it now sorts first
            source = listOf(source[4]) + source.filter { it.id.longValue !in setOf(3L, 5L) }

            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(requests.last()).isEqualTo(PageRequest(offset = 0, limit = 20))
            assertThat(pager.loadedIds())
                .isEqualTo(listOf(5L, 1L, 2L, 4L, 6L, 7L, 8L, 9L, 10L) + (11L..21L))
            assertThat(pager.state.value.reconcileVersion).isEqualTo(1)
        }

    @Test
    fun `test that the next page after a re-read starts at the re-read's nextOffset`() =
        pagerTest { pager ->
            source = source.filter { it.id.longValue != 3L }

            changes.emit(Unit)
            advanceUntilIdle()
            visibleRange.emit(5..9)
            advanceUntilIdle()

            assertThat(requests.map { it.offset }).containsExactly(0, 0, 10).inOrder()
            assertThat(pager.loadedIds()).isEqualTo((1L..25L).filter { it != 3L }.take(20))
        }

    @Test
    fun `test that a node appended to a fully loaded folder is loaded after the re-read`() {
        source = nodes(1L..10L)
        pagerTest { pager ->
            visibleRange.emit(5..9)
            advanceUntilIdle()
            assertThat(pager.state.value.hasMore).isFalse()

            source = source + node(11L)
            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(pager.loadedIds()).isEqualTo((1L..11L).toList())
        }
    }

    @Test
    fun `test that a change while the gate is closed waits and load-more still runs`() {
        publishGate.value = false
        pagerTest { pager ->
            changes.emit(Unit)
            visibleRange.emit(5..9)
            advanceUntilIdle()

            assertThat(requests).containsExactly(
                PageRequest(offset = 0, limit = 10),
                PageRequest(offset = 10, limit = 10),
            )
            assertThat(pager.state.value.reconcileVersion).isEqualTo(0)

            publishGate.value = true
            advanceUntilIdle()

            assertThat(requests.last()).isEqualTo(PageRequest(offset = 0, limit = 20))
            assertThat(pager.state.value.reconcileVersion).isEqualTo(1)
        }
    }

    @Test
    fun `test that a change during an in-flight page re-reads the prefix including that page`() =
        pagerTest { pager ->
            val hold = CompletableDeferred<Unit>()
            holdNextLoad = hold
            visibleRange.emit(5..9)
            settle()
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.LoadingMore)

            changes.emit(Unit)
            runCurrent()
            hold.complete(Unit)
            advanceUntilIdle()

            assertThat(requests.map { it.offset to it.limit })
                .containsExactly(0 to 10, 10 to 10, 0 to 20).inOrder()
            assertThat(pager.state.value.reconcileVersion).isEqualTo(1)
        }

    @Test
    fun `test that a burst of changes collapses into one re-read`() = pagerTest { _ ->
        repeat(3) { changes.emit(Unit) }
        advanceUntilIdle()

        assertThat(reReads()).hasSize(1)
    }

    @Test
    fun `test that an empty folder loads the first page again on a change`() {
        source = emptyList()
        pagerTest { pager ->
            assertThat(pager.loadedIds()).isEmpty()
            assertThat(pager.state.value.hasMore).isFalse()

            source = nodes(1L..3L)
            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(requests).containsExactly(
                PageRequest(offset = 0, limit = 10),
                PageRequest(offset = 0, limit = 10),
            )
            assertThat(pager.loadedIds()).isEqualTo(listOf(1L, 2L, 3L))
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.Idle)
        }
    }

    @Test
    fun `test that a change after a failed first page loads it again`() {
        failNextLoad = true
        pagerTest { pager ->
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.FirstPageFailed)

            changes.emit(Unit)
            runCurrent()

            assertThat(requests).hasSize(2)
            assertThat(pager.loadedIds()).hasSize(10)
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.Idle)
        }
    }

    @Test
    fun `test that an empty re-read empties the list`() =
        pagerTest(withCount = true) { pager ->
            source = emptyList()

            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(reReads()).hasSize(1)
            assertThat(pager.loadedIds()).isEmpty()
            assertThat(pager.state.value.hasMore).isFalse()
        }

    @Test
    fun `test that retry loads a failed first page`() {
        failNextLoad = true
        pagerTest { pager ->
            pager.retry()
            runCurrent()

            assertThat(requests).hasSize(2)
            assertThat(pager.loadedIds()).hasSize(10)
        }
    }

    @Test
    fun `test that scrolling further retries a failed page`() = pagerTest { pager ->
        failNextLoad = true
        visibleRange.emit(5..9)
        settle()
        assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.PageFailed)

        visibleRange.emit(10..14)
        settle()

        assertThat(requests).hasSize(3)
        assertThat(pager.loadedIds()).hasSize(20)
    }

    @Test
    fun `test that a failed re-read keeps the items and the next change re-reads again`() =
        pagerTest { pager ->
            failNextLoad = true

            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(reReads()).hasSize(1)
            assertThat(pager.loadedIds()).hasSize(10)
            assertThat(pager.state.value.reconcileVersion).isEqualTo(0)

            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(reReads()).hasSize(2)
            assertThat(pager.state.value.reconcileVersion).isEqualTo(1)
        }

    @Test
    fun `test that a failed first page is reported and stays until retried`() {
        failNextLoad = true
        pagerTest { pager ->
            advanceUntilIdle()

            assertThat(requests).hasSize(1)
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.FirstPageFailed)
            assertThat(pager.loadedIds()).isEmpty()
        }
    }

    @Test
    fun `test that a failed later page keeps the items and is not retried by itself`() =
        pagerTest { pager ->
            failNextLoad = true

            visibleRange.emit(5..9)
            advanceUntilIdle()

            assertThat(requests).hasSize(2)
            assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.PageFailed)
            assertThat(pager.loadedIds()).hasSize(10)
        }

    @Test
    fun `test that cancelling run during a page does not report a failure`() = runTest {
        val pager = newPager()
        val job = launch { pager.run(visibleRange, changes, publishGate) }
        runCurrent()
        holdNextLoad = CompletableDeferred()
        visibleRange.emit(5..9)
        settle()
        assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.LoadingMore)

        job.cancel()
        advanceUntilIdle()

        assertThat(pager.state.value.loadState).isEqualTo(PagedLoadState.LoadingMore)
        assertThat(pager.loadedIds()).hasSize(10)
    }

    @Test
    fun `test that total is read with the first page and again on a re-read`() =
        pagerTest(withCount = true) { pager ->
            assertThat(pager.state.value.total).isEqualTo(25)

            source = source + node(26L)
            changes.emit(Unit)
            advanceUntilIdle()

            assertThat(pager.state.value.total).isEqualTo(26)
        }

    @Test
    fun `test that total is null when the pager has no count`() = pagerTest { pager ->
        assertThat(pager.state.value.total).isNull()
    }
}
