package mega.privacy.mobile.home.presentation.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetCurrentVersionNodeUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffSortPreferenceUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SetContinueWhereLeftOffSortUseCase
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
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
    private val monitorContinueWhereLeftOffSortPreferenceUseCase =
        mock<MonitorContinueWhereLeftOffSortPreferenceUseCase>()
    private val setContinueWhereLeftOffSortUseCase =
        mock<SetContinueWhereLeftOffSortUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getCurrentVersionNodeUseCase = mock<GetCurrentVersionNodeUseCase>()
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
        stubDefaultSortPreference()
        underTest = ContinueWhereLeftOffListViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            monitorContinueWhereLeftOffSortPreferenceUseCase = monitorContinueWhereLeftOffSortPreferenceUseCase,
            setContinueWhereLeftOffSortUseCase = setContinueWhereLeftOffSortUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getCurrentVersionNodeUseCase = getCurrentVersionNodeUseCase,
            clearRecentlyUsedItemsUseCase = clearRecentlyUsedItemsUseCase,
            nameResolver = ContinueWhereLeftOffNameResolver(
                getNodeByIdUseCase,
                DurationInSecondsTextMapper(),
                mock(),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorContinueWhereLeftOffItemsUseCase,
            monitorContinueWhereLeftOffSortPreferenceUseCase,
            setContinueWhereLeftOffSortUseCase,
            getNodeByIdUseCase,
            getCurrentVersionNodeUseCase,
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
    fun `test that sort configuration reflects timestamp descending preference`() = runTest {
        stubSortPreference(
            ContinueWhereLeftOffSortField.Timestamp,
            SortDirection.Descending,
        )
        stubEmptyItems()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.sortConfiguration.sortOption).isEqualTo(NodeSortOption.LastAccessed)
            assertThat(state.sortConfiguration.sortDirection).isEqualTo(SortDirection.Descending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are displayed in the order returned by use case`() = runTest {
        stubItems(sampleItems)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items.map { it.nodeHandle })
                .containsExactly(1L, 2L, 3L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items use case is called with list limit`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem()
            verify(monitorContinueWhereLeftOffItemsUseCase).invoke(
                limit = eq(50),
                sortField = isNull(),
                sortDirection = isNull(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updateSortConfiguration persists LastAccessed descending via use case`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem() // initial
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.LastAccessed, SortDirection.Descending)
            )
            cancelAndIgnoreRemainingEvents()
        }
        verify(setContinueWhereLeftOffSortUseCase).invoke(
            ContinueWhereLeftOffSortField.Timestamp,
            SortDirection.Descending,
        )
    }

    @Test
    fun `test that updateSortConfiguration persists Name descending via use case`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            awaitItem() // initial
            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending)
            )
            cancelAndIgnoreRemainingEvents()
        }
        verify(setContinueWhereLeftOffSortUseCase).invoke(
            ContinueWhereLeftOffSortField.Name,
            SortDirection.Descending,
        )
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
            underTest.onItemClicked(nodeHandle, RecentlyUsedType.Video)
            val state = awaitItem()
            assertThat(state.openNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.openNodeEvent as StateEventWithContentTriggered).content)
                .isEqualTo(expectedNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked resolves current version when item is a text editor`() = runTest {
        stubEmptyItems()
        val nodeHandle = 123L
        val currentVersion = mock<TypedFileNode>()
        whenever(getCurrentVersionNodeUseCase(NodeId(nodeHandle))).thenReturn(currentVersion)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.onItemClicked(nodeHandle, RecentlyUsedType.TextEditor)
            val state = awaitItem()
            assertThat(state.openNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.openNodeEvent as StateEventWithContentTriggered).content)
                .isEqualTo(currentVersion)
            cancelAndIgnoreRemainingEvents()
        }
        verify(getCurrentVersionNodeUseCase).invoke(NodeId(nodeHandle))
    }

    @Test
    fun `test that onItemClicked does not trigger event when use case throws`() = runTest {
        stubEmptyItems()
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        underTest.uiState.test {
            val state = awaitItem()
            underTest.onItemClicked(nodeHandle, RecentlyUsedType.Video)
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
            underTest.onItemClicked(nodeHandle, RecentlyUsedType.Video)
            awaitItem() // triggered
            underTest.onOpenNodeEventConsumed()
            val state = awaitItem()
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubDefaultSortPreference() {
        stubSortPreference(
            ContinueWhereLeftOffSortField.Name,
            SortDirection.Ascending,
        )
    }

    private fun stubSortPreference(
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    ) {
        whenever(monitorContinueWhereLeftOffSortPreferenceUseCase())
            .thenReturn(flowOf(sortField to sortDirection))
    }

    private fun stubEmptyItems() {
        whenever(
            monitorContinueWhereLeftOffItemsUseCase(any(), anyOrNull(), anyOrNull())
        ) doReturn flow {
            emit(emptyList<ContinueWhereLeftOffItem>())
            awaitCancellation()
        }
    }

    private fun stubItems(items: List<ContinueWhereLeftOffItem>) {
        whenever(
            monitorContinueWhereLeftOffItemsUseCase(any(), anyOrNull(), anyOrNull())
        ) doReturn flow {
            emit(items)
            awaitCancellation()
        }
    }

    private fun stubFakeFlow(): MutableSharedFlow<List<ContinueWhereLeftOffItem>> {
        val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()
        whenever(
            monitorContinueWhereLeftOffItemsUseCase(any(), anyOrNull(), anyOrNull())
        ) doReturn fakeFlow
        return fakeFlow
    }
}
