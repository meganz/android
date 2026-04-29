package mega.privacy.mobile.home.presentation.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContinueWhereLeftOffListViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }

    private val monitorContinueWhereLeftOffItemsUseCase =
        mock<MonitorContinueWhereLeftOffItemsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val clearRecentlyUsedItemsUseCase = mock<ClearRecentlyUsedItemsUseCase>()
    private val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()

    private lateinit var underTest: ContinueWhereLeftOffListViewModel

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
        whenever(monitorContinueWhereLeftOffItemsUseCase(50)).thenReturn(fakeFlow)
        underTest = ContinueWhereLeftOffListViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            clearRecentlyUsedItemsUseCase = clearRecentlyUsedItemsUseCase,
        )
    }

    @Test
    fun `test that isLoading is initially true`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isLoading becomes false after first emission`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            fakeFlow.emit(emptyList())
            assertThat(awaitItem().isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that initial items is empty list`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that default sort is Name ascending`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.sortConfiguration.sortOption).isEqualTo(NodeSortOption.Name)
            assertThat(state.sortConfiguration.sortDirection).isEqualTo(SortDirection.Ascending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by name ascending by default`() = runTest {
        underTest.uiState.test {
            awaitItem() // initial empty
            fakeFlow.emit(sampleItems)
            val state = awaitItem()
            // Name ASC: Alpha(2), Bravo(3), Charlie(1)
            assertThat(state.items.map { it.nodeHandle }).containsExactly(2L, 3L, 1L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are sorted by name descending when updated`() = runTest {
        underTest.uiState.test {
            awaitItem() // initial empty
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
        underTest.uiState.test {
            awaitItem() // initial empty
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
        underTest.uiState.test {
            awaitItem() // initial empty
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
        underTest.uiState.test {
            awaitItem() // initial — triggers lazy init
            underTest.clearAll()
            testScheduler.advanceUntilIdle()
            verify(clearRecentlyUsedItemsUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clearAll dismisses options sheet`() = runTest {
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
        underTest.uiState.test {
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from LIST to GRID`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.LIST)
            underTest.onChangeViewTypeClicked()
            assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from GRID back to LIST`() = runTest {
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
    fun `test that onItemClicked triggers openNodeEvent when node is resolved`() = runTest {
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial state
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
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        underTest.onItemClicked(nodeHandle)

        assertThat(underTest.uiState.value.openNodeEvent)
            .isInstanceOf(StateEventWithContentConsumed::class.java)
    }

    @Test
    fun `test that onOpenNodeEventConsumed resets the event`() = runTest {
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial state
            underTest.onItemClicked(nodeHandle)
            awaitItem() // triggered state
            underTest.onOpenNodeEventConsumed()
            val state = awaitItem()
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
