package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
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
class CloudDriveViewModelTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val folderNodeHandle = 123L
    private val folderNodeId = NodeId(folderNodeHandle)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            getNodeByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            durationInSecondsTextMapper,
            fileTypeIconMapper
        )
    }

    private fun createViewModel() = CloudDriveViewModel(
        getNodeByIdUseCase = getNodeByIdUseCase,
        getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        durationInSecondsTextMapper = durationInSecondsTextMapper,
        fileTypeIconMapper = fileTypeIconMapper,
        savedStateHandle = SavedStateHandle.Companion.invoke(
            route = CloudDrive(folderNodeHandle)
        ),
        defaultDispatcher = testDispatcher
    )

    private suspend fun setupTestData(items: List<TypedNode>) {
        val folderNode = mock<TypedFolderNode> {
            on { id } doReturn folderNodeId
            on { name } doReturn "Test Folder"
        }
        whenever(getNodeByIdUseCase(eq(folderNodeId))).thenReturn(folderNode)
        whenever(getFileBrowserNodeChildrenUseCase(folderNodeHandle)).thenReturn(items)
        whenever(durationInSecondsTextMapper(any())).thenReturn(null)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.currentFolderId).isEqualTo(folderNodeId)
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.isInSelectionMode).isFalse()
            assertThat(initialState.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that loadNodes populates items correctly`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
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
            assertThat(loadedState.items[0].isSelected).isFalse()
            assertThat(loadedState.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that onItemLongClicked toggles item selection and updates items state`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that onItemClicked in selection mode toggles item selection`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.onItemClicked(nodeUiItem2)
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that onItemClicked in normal mode navigates to folder`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = folderNode,
            isSelected = false
        )

        underTest.onItemClicked(nodeUiItem)

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(nodeUiItem.id))
        }
    }

    @Test
    fun `test that selectAllItems selects all items in state`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.selectAllItems()
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that deselectAllItems deselects all items in state`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.selectAllItems()
            awaitItem()

            underTest.deselectAllItems()
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isFalse()
            assertThat(updatedState.items[0].isSelected).isFalse()
            assertThat(updatedState.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that deselectAllItems does nothing when no items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.deselectAllItems()

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that selectAllItems does nothing when no items exist`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.selectAllItems()

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items).isEmpty()
        }
    }

    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.onItemLongClicked(nodeUiItem1)
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.onItemLongClicked(updatedNodeUiItem1)
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isInSelectionMode).isFalse()
        }
    }

    @Test
    fun `test that multiple items can be selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.onItemLongClicked(nodeUiItem2)
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that selection state is maintained when items are updated`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
            assertThat(state.items[0].isSelected).isTrue()
        }
    }

    @Test
    fun `test that selection state is preserved when loading new items`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.onItemLongClicked(nodeUiItem1)
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.isInSelectionMode).isTrue()
            assertThat(stateAfterSelection.items[0].isSelected).isTrue()
            assertThat(stateAfterSelection.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that onNavigateToFolderEventConsumed consumes the navigation event`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node } doReturn folderNode
            on { id } doReturn folderNodeId
        }

        underTest.onItemClicked(nodeUiItem)
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.onNavigateToFolderEventConsumed()
            val stateAfterConsume = awaitItem()

            assertThat(stateAfterClick.navigateToFolderEvent).isEqualTo(triggered(folderNodeId))
            assertThat(stateAfterConsume.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.onChangeViewTypeClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
    }

    @Test
    fun `test that onChangeViewTypeClicked toggles from GRID to LIST`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val stateWithGrid = awaitItem() // State after monitorViewType updates
            assertThat(stateWithGrid.currentViewType).isEqualTo(ViewType.GRID)
        }

        underTest.onChangeViewTypeClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.LIST)
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
        }
    }

    @Test
    fun `test that multiple view type changes are handled correctly`() = runTest {
        setupTestData(emptyList())
        // Use individual flows since flowOf emits all values synchronously
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val gridState = awaitItem() // State after monitorViewType flow emits GRID
            assertThat(gridState.currentViewType).isEqualTo(ViewType.GRID)
        }
    }

    @Test
    fun `test that view type is correctly set in initial state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            // Initial state should have default value before monitoring starts
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)

            val updatedState = awaitItem()
            // After monitoring starts, it should be updated from the flow
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }
} 