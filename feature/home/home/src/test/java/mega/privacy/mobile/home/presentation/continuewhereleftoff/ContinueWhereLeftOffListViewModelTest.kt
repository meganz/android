package mega.privacy.mobile.home.presentation.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContinueWhereLeftOffListViewModelTest {

    private lateinit var underTest: ContinueWhereLeftOffListViewModel

    private val monitorContinueWhereLeftOffItemsUseCase =
        mock<MonitorContinueWhereLeftOffItemsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val clearRecentlyUsedItemsUseCase = mock<ClearRecentlyUsedItemsUseCase>()

    private val sampleItems = listOf(
        ContinueWhereLeftOffItem(
            nodeHandle = 1L,
            type = RecentlyUsedType.PDF,
            title = "Charlie.pdf",
            lastAccessedTimestamp = 2000L,
        ),
        ContinueWhereLeftOffItem(
            nodeHandle = 2L,
            type = RecentlyUsedType.Audio,
            title = "Alpha.mp3",
            lastAccessedTimestamp = 1000L,
        ),
        ContinueWhereLeftOffItem(
            nodeHandle = 3L,
            type = RecentlyUsedType.Video,
            title = "Bravo.mp4",
            lastAccessedTimestamp = 3000L,
        ),
    )

    @BeforeEach
    fun setUp() {
        underTest = ContinueWhereLeftOffListViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            clearRecentlyUsedItemsUseCase = clearRecentlyUsedItemsUseCase,
            nameResolver = ContinueWhereLeftOffNameResolver(getNodeByIdUseCase),
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase,
            clearRecentlyUsedItemsUseCase,
        )
    }

    @Test
    fun `test that isLoading is initially true`() = runTest {
        stubFakeFlow() // no emission yet → combine doesn't fire → initial value

        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isLoading becomes false after first emission`() = runTest {
        val fakeFlow = stubFakeFlow()

        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            fakeFlow.emit(emptyList())
            assertThat(awaitItem().isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that initial items is empty list`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that default sort is Name ascending`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.sortConfiguration.sortOption).isEqualTo(NodeSortOption.Name)
            assertThat(state.sortConfiguration.sortDirection).isEqualTo(SortDirection.Ascending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by name ascending by default`() = runTest {
        stubItems(sampleItems)

        underTest.uiState.test {
            val state = awaitItem()
            // Name ASC: Alpha(2), Bravo(3), Charlie(1)
            assertThat(state.items.map { it.nodeHandle }).containsExactly(2L, 3L, 1L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by name descending when updated`() = runTest {
        val fakeFlow = stubFakeFlow()

        underTest.uiState.test {
            awaitItem() // initial
            fakeFlow.emit(sampleItems)
            awaitItem() // default sort
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending)
            )
            val state = awaitItem()
            // Name DESC: Charlie(1), Bravo(3), Alpha(2)
            assertThat(state.items.map { it.title })
                .containsExactly("Charlie.pdf", "Bravo.mp4", "Alpha.mp3").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by created descending`() = runTest {
        val fakeFlow = stubFakeFlow()

        underTest.uiState.test {
            awaitItem() // initial
            fakeFlow.emit(sampleItems)
            awaitItem() // default sort
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Descending)
            )
            val state = awaitItem()
            // Created DESC: 3→3000, 1→2000, 2→1000
            assertThat(state.items.map { it.nodeHandle }).containsExactly(3L, 1L, 2L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by created ascending`() = runTest {
        val fakeFlow = stubFakeFlow()

        underTest.uiState.test {
            awaitItem() // initial
            fakeFlow.emit(sampleItems)
            awaitItem() // default sort
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Ascending)
            )
            val state = awaitItem()
            // Created ASC: 2→1000, 1→2000, 3→3000
            assertThat(state.items.map { it.nodeHandle }).containsExactly(2L, 1L, 3L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sort configuration updates when updateSortConfiguration is called`() = runTest {
        stubEmptyItems()
        val newConfig = NodeSortConfiguration(NodeSortOption.Created, SortDirection.Descending)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.updateSortConfiguration(newConfig)
            assertThat(awaitItem().sortConfiguration).isEqualTo(newConfig)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updateSortConfiguration dismisses sort sheet`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem() // initial
            underTest.showSortSheet()
            assertThat(awaitItem().showSortSheet).isTrue()
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
            )
            assertThat(awaitItem().showSortSheet).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clearAll calls ClearRecentlyUsedItemsUseCase`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem() // triggers lazy init
            underTest.clearAll()
            verify(clearRecentlyUsedItemsUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clearAll dismisses options sheet`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem() // initial
            underTest.showOptionsSheet()
            assertThat(awaitItem().showOptionsSheet).isTrue()
            underTest.clearAll()
            assertThat(awaitItem().showOptionsSheet).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that default view type is LIST`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from LIST to GRID`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            underTest.onChangeViewTypeClicked()
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from GRID back to LIST`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            underTest.onChangeViewTypeClicked()
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
            underTest.onChangeViewTypeClicked()
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that blank title is resolved from node when available`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 10L,
                type = RecentlyUsedType.PDF,
                title = "",
                lastAccessedTimestamp = 1000L,
            )
        )
        val typedNode = mock<TypedFileNode> {
            on { name }.thenReturn("actual_name.pdf")
        }
        whenever(getNodeByIdUseCase(NodeId(10L))).thenReturn(typedNode)
        stubItems(items)

        underTest.uiState.test {
            // UnconfinedTestDispatcher resolves synchronously
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].title).isEqualTo("actual_name.pdf")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that non-blank title is not resolved from node`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 20L,
                type = RecentlyUsedType.Video,
                title = "stored_name.mp4",
                lastAccessedTimestamp = 2000L,
            )
        )
        stubItems(items)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].title).isEqualTo("stored_name.mp4")
            verify(getNodeByIdUseCase, never()).invoke(NodeId(20L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resolved name is cached and not fetched again`() = runTest {
        val fakeFlow = stubFakeFlow()
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 30L,
                type = RecentlyUsedType.PDF,
                title = "",
                lastAccessedTimestamp = 1000L,
            )
        )
        val typedNode = mock<TypedFileNode> {
            on { name }.thenReturn("resolved.pdf")
        }
        whenever(getNodeByIdUseCase(NodeId(30L))).thenReturn(typedNode)

        underTest.uiState.test {
            awaitItem() // initial
            fakeFlow.emit(items)
            awaitItem() // blank name (first transformLatest emit)
            assertThat(awaitItem().items[0].title).isEqualTo("resolved.pdf")
            fakeFlow.emit(listOf(items[0].copy(lastAccessedTimestamp = 2000L)))
            // cached name applied immediately in first transformLatest emit
            assertThat(awaitItem().items[0].title).isEqualTo("resolved.pdf")
            verify(getNodeByIdUseCase, times(1)).invoke(NodeId(30L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked triggers openNodeEvent when node is resolved`() = runTest {
        stubEmptyItems()
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.onItemClicked(nodeHandle)
            val state = awaitItem()
            assertThat(state.openNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.openNodeEvent as StateEventWithContentTriggered).content)
                .isEqualTo(expectedNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked does not trigger event when use case throws`() = runTest {
        stubEmptyItems()
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        underTest.uiState.test {
            val state = awaitItem()
            underTest.onItemClicked(nodeHandle)
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onOpenNodeEventConsumed resets the event`() = runTest {
        stubEmptyItems()
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.onItemClicked(nodeHandle)
            awaitItem() // triggered
            underTest.onOpenNodeEventConsumed()
            val state = awaitItem()
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubEmptyItems() {
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(50) } doReturn flow {
                emit(emptyList<ContinueWhereLeftOffItem>())
                awaitCancellation()
            }
        }
    }

    private fun stubItems(items: List<ContinueWhereLeftOffItem>) {
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(50) } doReturn flow {
                emit(items)
                awaitCancellation()
            }
        }
    }

    private fun stubFakeFlow(): MutableSharedFlow<List<ContinueWhereLeftOffItem>> {
        val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(50) } doReturn fakeFlow
        }
        return fakeFlow
    }
}
