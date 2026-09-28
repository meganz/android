package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.FolderPreferenceKeys
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.publiclink.GetPublicLinksUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksAction
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeViewItem
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LinksViewModelTest {
    private val getPublicLinksUseCase: GetPublicLinksUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorFolderViewTypeUseCase: MonitorFolderViewTypeUseCase = mock()
    private val setFolderViewTypeUseCase: SetFolderViewTypeUseCase = mock()
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase = mock()
    private val setFolderSortOrderUseCase: SetFolderSortOrderUseCase = mock()
    private val nodeViewItemMapper: NodeViewItemMapper = mock()
    private val monitorLinksSortOrderUseCase: MonitorLinksSortOrderUseCase = mock()
    private val setCloudSortOrder: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()

    private lateinit var testScheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            getPublicLinksUseCase,
            monitorNodeUpdatesByIdUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorFolderViewTypeUseCase,
            setFolderViewTypeUseCase,
            monitorFolderSortOrderUseCase,
            setFolderSortOrderUseCase,
            nodeViewItemMapper,
            monitorLinksSortOrderUseCase,
            setCloudSortOrder,
            nodeSortConfigurationUiMapper
        )
    }

    private fun createViewModel() = LinksViewModel(
        getPublicLinksUseCase = getPublicLinksUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorFolderViewTypeUseCase = monitorFolderViewTypeUseCase,
        setFolderViewTypeUseCase = setFolderViewTypeUseCase,
        monitorFolderSortOrderUseCase = monitorFolderSortOrderUseCase,
        setFolderSortOrderUseCase = setFolderSortOrderUseCase,
        nodeViewItemMapper = nodeViewItemMapper,
        monitorLinksSortOrderUseCase = monitorLinksSortOrderUseCase,
        setCloudSortOrder = setCloudSortOrder,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
    )

    private suspend fun setupTestData(items: List<TypedNode>) {
        whenever(monitorLinksSortOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(NodeSortConfiguration.default)
        whenever(monitorFolderViewTypeUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
        whenever(monitorFolderSortOrderUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
        whenever(getPublicLinksUseCase(anyOrNull())).thenReturn(items)
        whenever(
            monitorNodeUpdatesByIdUseCase(NodeId(-1L), NodeSourceType.LINKS)
        ).thenReturn(flowOf())

        val nodeViewItems = items.map { node -> NodeViewItem<TypedNode>(node = node) }
        whenever(
            nodeViewItemMapper(
                nodeList = items,
                nodeSourceType = NodeSourceType.LINKS,
                isContactVerificationOn = false,
                highlightedNodeId = null,
                highlightedNames = null,
                isHiddenNodesEnabled = false,
            )
        ).thenReturn(nodeViewItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that loadLinks populates items correctly`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.items).hasSize(2)
            assertThat(loadedState.items[0].node.id).isEqualTo(NodeId(1L))
            assertThat(loadedState.items[1].node.id).isEqualTo(NodeId(2L))
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.processAction(LinksAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.LINKS),
            viewType = eq(ViewType.GRID),
            currentSortOrder = any(),
            orElse = any(),
        )
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from GRID to LIST`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val stateWithGrid = awaitItem() // State after monitorViewType updates
            assertThat(stateWithGrid.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }

        underTest.processAction(LinksAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.LINKS),
            viewType = eq(ViewType.LIST),
            currentSortOrder = any(),
            orElse = any(),
        )
    }

    @Test
    fun `test that monitorViewType updates currentViewType in ui state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val updatedState = awaitItem() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that monitorViewType handles GRID view type correctly`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val updatedState = awaitItem() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSortOrder updates selectedSort in UI state`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_SIZE_DESC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(monitorLinksSortOrderUseCase()).thenReturn(flowOf(expectedSortOrder))
        whenever(nodeSortConfigurationUiMapper(expectedSortOrder)).thenReturn(
            expectedSortConfiguration
        )

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedSortConfiguration).isEqualTo(expectedSortConfiguration)
            assertThat(state.selectedSortOrder).isEqualTo(expectedSortOrder)
        }
    }

    @Test
    fun `test that setSortOrder writes via setFolderSortOrderUseCase`() = runTest {
        setupTestData(emptyList())
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setSortOrder(sortConfiguration)
        advanceUntilIdle()

        verify(setFolderSortOrderUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.LINKS),
            sortOrder = eq(expectedSortOrder),
            currentViewType = any(),
            orElse = any(),
        )
    }

    @Test
    fun `test that isEmpty returns true when no items and not loading`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isEmpty).isTrue()
        }
    }

    @Test
    fun `test that isEmpty returns false when loading`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isEmpty).isFalse()
        }
    }

    @Test
    fun `test that isEmpty returns false when items exist`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isEmpty).isFalse()
        }
    }

}
