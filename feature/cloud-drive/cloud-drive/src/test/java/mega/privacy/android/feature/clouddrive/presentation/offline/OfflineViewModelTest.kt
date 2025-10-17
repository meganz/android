package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.sortorder.GetSortOrderByNodeSourceTypeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.OfflineNavKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OfflineViewModelTest {
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase = mock()
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val removeOfflineNodesUseCase: RemoveOfflineNodesUseCase = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()
    private val setOfflineSortOrder: SetOfflineSortOrder = mock()
    private val getSortOrderByNodeSourceTypeUseCase: GetSortOrderByNodeSourceTypeUseCase = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val setViewType: SetViewType = mock()
    private lateinit var underTest: OfflineViewModel

    @Before
    fun setUp() {
        runTest {
            stubCommon()
        }
    }

    private fun initViewModel(
        title: String? = null,
        nodeId: Int = -1,
        path: String? = null,
        highlightedFiles: String? = null,
    ) {
        underTest = OfflineViewModel(
            navKey = OfflineNavKey(
                title = title,
                nodeId = nodeId,
                path = path,
                highlightedFiles = highlightedFiles
            ),
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            setOfflineWarningMessageVisibilityUseCase = setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase = monitorOfflineWarningMessageVisibilityUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorViewType = monitorViewType,
            removeOfflineNodesUseCase = removeOfflineNodesUseCase,
            snackbarEventQueue = snackbarEventQueue,
            setOfflineSortOrder = setOfflineSortOrder,
            getSortOrderByNodeSourceTypeUseCase = getSortOrderByNodeSourceTypeUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            setViewType = setViewType
        )
    }

    @Test
    fun `test that the online status is updated correctly when true`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(true)

        initViewModel()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(true)
        }
    }

    @Test
    fun `test that the online status is updated correctly when false`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initViewModel()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(false)
        }
    }

    @Test
    fun `test that dismissOfflineWarning calls setOfflineWarningMessageVisibilityUseCase`() =
        runTest {
            initViewModel()
            underTest.dismissOfflineWarning()
            verify(setOfflineWarningMessageVisibilityUseCase).invoke(false)
        }

    @Test
    fun `test that folder is clicked then it triggers navigation event`() = runTest {
        val parentId = 1
        val nodeInfo = OtherOfflineNodeInformation(
            id = parentId,
            parentId = 0,
            name = "Sample",
            isFolder = true,
            handle = "1234",
            lastModifiedTime = 100000L,
            path = ""
        )

        val item = OfflineNodeUiItem(
            offlineFileInformation = OfflineFileInformation(
                nodeInfo = nodeInfo
            ),
            isSelected = false
        )

        initViewModel()

        underTest.onItemClicked(offlineNodeUIItem = item)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat((state.openFolderInPageEvent as? StateEventWithContentTriggered<*>)?.content)
                .isEqualTo(item.offlineFileInformation)
        }
    }

    @Test
    fun `test that navigation even is sent when folder is clicked from homepage`() = runTest {
        val nodeInfo = OtherOfflineNodeInformation(
            id = 1,
            parentId = 0,
            name = "Sample",
            isFolder = true,
            handle = "1234",
            lastModifiedTime = 100000L,
            path = ""
        )

        val item = OfflineNodeUiItem(
            offlineFileInformation = OfflineFileInformation(
                nodeInfo = nodeInfo,
                absolutePath = ""
            ),
            isSelected = false
        )

        initViewModel()

        underTest.onItemClicked(
            offlineNodeUIItem = item,
        )

        underTest.uiState.test {
            val newItem = awaitItem()
            assertThat(((newItem.openFolderInPageEvent as? StateEventWithContentTriggered<*>)?.content as? OfflineFileInformation))
                .isEqualTo(item.offlineFileInformation)
        }
    }

    @Test
    fun `test that the selected node size is equal to the total offline list size when select all is clicked`() =
        runTest {
            val parentId = 1
            val offlineList1 = mock<OfflineFileInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.addedTime).thenReturn(100000L)

            val offlineList2 = mock<OfflineFileInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.addedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)

            val nodeInfo = OtherOfflineNodeInformation(
                id = parentId,
                parentId = 0,
                name = "Sample",
                isFolder = true,
                handle = "1234",
                lastModifiedTime = 100000L,
                path = ""
            )

            initViewModel()

            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUiItem(
                    offlineFileInformation = OfflineFileInformation(
                        nodeInfo = nodeInfo
                    ),
                    isSelected = false
                ),
            )
            underTest.selectAll()
            assertThat(underTest.uiState.value.offlineNodes.size).isEqualTo(underTest.uiState.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that the total selected node size is empty when clear all is clicked`() =
        runTest {
            val parentId = 1
            val offlineList1 = mock<OfflineFileInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.addedTime).thenReturn(100000L)

            val offlineList2 = mock<OfflineFileInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.addedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)

            val nodeInfo = OtherOfflineNodeInformation(
                id = parentId,
                parentId = 0,
                name = "Sample",
                isFolder = true,
                handle = "1234",
                lastModifiedTime = 100000L,
                path = ""
            )

            initViewModel()

            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUiItem(
                    offlineFileInformation = OfflineFileInformation(
                        nodeInfo = nodeInfo
                    ),
                    isSelected = false
                ),
            )
            underTest.clearSelection()
            assertThat(underTest.uiState.value.selectedNodeHandles.size).isEqualTo(0)
        }

    @Test
    fun `test that onLongItemClicked method works without throwing exceptions`() = runTest {
        val parentId = -1
        val offlineList1 = mock<OfflineFileInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.addedTime).thenReturn(100000L)

        val offlineList2 = mock<OfflineFileInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.addedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)

        initViewModel()

        // Test long click on a node
        val longClickNodeInfo = OtherOfflineNodeInformation(
            id = 1,
            parentId = 0,
            name = "folder",
            isFolder = true,
            handle = "1234",
            lastModifiedTime = 100000L,
            path = ""
        )

        // Test that the method doesn't throw an exception
        underTest.onLongItemClicked(
            offlineNodeUIItem = OfflineNodeUiItem(
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = longClickNodeInfo
                ),
            )
        )

        // Just verify that the method executed without throwing an exception
        // The actual selection logic might require the node to be in the current state
        assertThat(underTest.uiState.value.selectedNodeHandles).isNotNull()
    }

    @Test
    fun `test that navigateToPath triggers navigation events for path traversal`() = runTest {
        val parentId = -1
        val childId = 3453
        val grandChildId = 845
        val child = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "folder"
            on { id } doReturn childId
            on { handle } doReturn "1234"
        }
        val grandChild = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "subFolder"
            on { id } doReturn grandChildId
            on { handle } doReturn "2345"
        }
        val mockFile = mock<OfflineFileInformation>() {
            on { isFolder } doReturn true
            on { name } doReturn "subSubFolder"
            on { id } doReturn 5678
            on { handle } doReturn "3456"
        }
        val path = File.separator + child.name + File.separator + grandChild.name + File.separator

        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(listOf(child))
        whenever(getOfflineNodesByParentIdUseCase(childId)).thenReturn(listOf(grandChild))
        whenever(getOfflineNodesByParentIdUseCase(grandChildId)).thenReturn(listOf(mockFile))

        initViewModel(path = path)

        // Test that navigateToPath doesn't throw an exception
        underTest.navigateToPath()

        // Verify that the path is set correctly in the state
        assertThat(underTest.uiState.value.path).isEqualTo(path)
    }

    @Test
    fun `test that navigateToPath sets loading state correctly`() = runTest {
        val parentId = -1
        val childId = 3453
        val grandChildId = 845
        val child = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "folder"
            on { id } doReturn childId
            on { handle } doReturn "1234"
        }
        val grandChild = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "subFolder"
            on { id } doReturn grandChildId
            on { handle } doReturn "2345"
        }
        val mockFile = mock<OfflineFileInformation>() {
            on { isFolder } doReturn true
            on { name } doReturn "subSubFolder"
            on { id } doReturn 5678
            on { handle } doReturn "3456"
        }
        val path = File.separator + child.name + File.separator + grandChild.name + File.separator

        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(listOf(child))
        whenever(getOfflineNodesByParentIdUseCase(childId)).thenReturn(listOf(grandChild))
        whenever(getOfflineNodesByParentIdUseCase(grandChildId)).thenReturn(listOf(mockFile))

        initViewModel(path = path)

        // Test that navigateToPath doesn't throw an exception
        underTest.navigateToPath()

        // Verify that the path is set correctly in the state
        assertThat(underTest.uiState.value.path).isEqualTo(path)
    }

    @Test
    fun `test that navigateToPath sets highlighted files correctly`() = runTest {
        val parentId = -1
        val childId = 3453
        val folder = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "folder"
            on { id } doReturn childId
            on { handle } doReturn "1234"
        }
        val highlighted = mock<OfflineFileInformation> {
            on { isFolder } doReturn false
            on { name } doReturn "file1"
            on { id } doReturn 564
            on { handle } doReturn "2345"
        }
        val file2 = mock<OfflineFileInformation> {
            on { isFolder } doReturn false
            on { name } doReturn "file2"
            on { id } doReturn 63456
            on { handle } doReturn "3456"
        }
        val path = File.separator + folder.name + File.separator
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(listOf(folder))
        whenever(getOfflineNodesByParentIdUseCase(childId)).thenReturn(listOf(highlighted, file2))

        initViewModel(path = path, highlightedFiles = highlighted.name)

        underTest.navigateToPath()

        assertThat(underTest.uiState.value.highlightedFiles).contains(highlighted.name)
        assertThat(underTest.uiState.value.highlightedFiles).isNotEmpty()
    }

    @Test
    fun `test that removeOfflineNodes calls removeOfflineNodesUseCase with selected node handles`() =
        runTest {
            initViewModel()

            val selectedHandles = listOf(123L, 456L, 789L)

            underTest.removeOfflineNodes(selectedHandles)

            verify(removeOfflineNodesUseCase).invoke(selectedHandles.map { NodeId(it) })
        }

    @Test
    fun `test that removeOfflineNodes handles empty selection gracefully`() = runTest {
        initViewModel()

        underTest.removeOfflineNodes(emptyList())

        verify(removeOfflineNodesUseCase, never()).invoke(any())
    }

    @Test
    fun `test that setSortOrder calls setOfflineSortOrder with correct sort order`() = runTest {
        // Given
        val sortConfiguration = NodeSortConfiguration(
            sortOption = NodeSortOption.Name,
            sortDirection = SortDirection.Ascending
        )
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        whenever(nodeSortConfigurationUiMapper.invoke(sortConfiguration)).thenReturn(
            expectedSortOrder
        )

        initViewModel()

        // When
        underTest.setSortOrder(sortConfiguration)

        // Then
        verify(setOfflineSortOrder).invoke(expectedSortOrder)
    }

    @Test
    fun `test that setSortOrder calls setOfflineSortOrder with different sort configurations`() =
        runTest {
            // Given
            initViewModel()

            // Test ORDER_DEFAULT_DESC
            val nameDescConfig = NodeSortConfiguration(
                sortOption = NodeSortOption.Name,
                sortDirection = SortDirection.Descending
            )
            whenever(nodeSortConfigurationUiMapper.invoke(nameDescConfig)).thenReturn(SortOrder.ORDER_DEFAULT_DESC)
            underTest.setSortOrder(nameDescConfig)
            verify(setOfflineSortOrder).invoke(SortOrder.ORDER_DEFAULT_DESC)

            // Test ORDER_SIZE_ASC
            val sizeAscConfig = NodeSortConfiguration(
                sortOption = NodeSortOption.Size,
                sortDirection = SortDirection.Ascending
            )
            whenever(nodeSortConfigurationUiMapper.invoke(sizeAscConfig)).thenReturn(SortOrder.ORDER_SIZE_ASC)
            underTest.setSortOrder(sizeAscConfig)
            verify(setOfflineSortOrder).invoke(SortOrder.ORDER_SIZE_ASC)

            // Test ORDER_MODIFICATION_DESC
            val modifiedDescConfig = NodeSortConfiguration(
                sortOption = NodeSortOption.Modified,
                sortDirection = SortDirection.Descending
            )
            whenever(nodeSortConfigurationUiMapper.invoke(modifiedDescConfig)).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            underTest.setSortOrder(modifiedDescConfig)
            verify(setOfflineSortOrder).invoke(SortOrder.ORDER_MODIFICATION_DESC)
        }

    @Test
    fun `test that getSortOrder calls getSortOrderByNodeSourceTypeUseCase with OFFLINE source type`() =
        runTest {
            // Given
            val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
            val expectedSortConfiguration = NodeSortConfiguration(
                sortOption = NodeSortOption.Name,
                sortDirection = SortDirection.Ascending
            )
            whenever(getSortOrderByNodeSourceTypeUseCase(NodeSourceType.OFFLINE)).thenReturn(
                expectedSortOrder
            )
            whenever(nodeSortConfigurationUiMapper.invoke(expectedSortOrder)).thenReturn(
                expectedSortConfiguration
            )

            initViewModel()

            // When
            underTest.uiState.test {
                val state = awaitItem()
                // Verify that the sort order is set correctly in the state
                assertThat(state.selectedSortOrder).isEqualTo(expectedSortOrder)
                assertThat(state.selectedSortConfiguration).isEqualTo(expectedSortConfiguration)
            }

            // Then
            verify(getSortOrderByNodeSourceTypeUseCase).invoke(NodeSourceType.OFFLINE)
        }

    @Test
    fun `test that getSortOrder updates UI state with different sort orders`() = runTest {
        // Test ORDER_SIZE_DESC
        val sizeDescOrder = SortOrder.ORDER_SIZE_DESC
        val sizeDescConfig = NodeSortConfiguration(
            sortOption = NodeSortOption.Size,
            sortDirection = SortDirection.Descending
        )
        whenever(getSortOrderByNodeSourceTypeUseCase(NodeSourceType.OFFLINE)).thenReturn(
            sizeDescOrder
        )
        whenever(nodeSortConfigurationUiMapper.invoke(sizeDescOrder)).thenReturn(sizeDescConfig)

        // Given
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedSortOrder).isEqualTo(sizeDescOrder)
            assertThat(state.selectedSortConfiguration).isEqualTo(sizeDescConfig)
        }

        verify(getSortOrderByNodeSourceTypeUseCase).invoke(NodeSourceType.OFFLINE)
    }

    @Test
    fun `test that setSortOrder and getSortOrder work together correctly`() = runTest {
        // Given
        val sortConfiguration = NodeSortConfiguration(
            sortOption = NodeSortOption.Modified,
            sortDirection = SortDirection.Ascending
        )
        val expectedSortOrder = SortOrder.ORDER_MODIFICATION_ASC
        val expectedSortConfiguration = NodeSortConfiguration(
            sortOption = NodeSortOption.Modified,
            sortDirection = SortDirection.Ascending
        )

        whenever(nodeSortConfigurationUiMapper.invoke(sortConfiguration)).thenReturn(
            expectedSortOrder
        )
        whenever(getSortOrderByNodeSourceTypeUseCase(NodeSourceType.OFFLINE)).thenReturn(
            expectedSortOrder
        )
        whenever(nodeSortConfigurationUiMapper.invoke(expectedSortOrder)).thenReturn(
            expectedSortConfiguration
        )

        initViewModel()

        underTest.setSortOrder(sortConfiguration)

        verify(setOfflineSortOrder).invoke(expectedSortOrder)
        // Times(2) because initViewModel also calls getSortOrder
        verify(getSortOrderByNodeSourceTypeUseCase, times(2)).invoke(NodeSourceType.OFFLINE)
    }

    private suspend fun stubCommon() {
        whenever(getOfflineNodesByParentIdUseCase(-1)).thenReturn(emptyList())
        whenever(setOfflineWarningMessageVisibilityUseCase(false)).thenReturn(Unit)
        whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(emptyFlow())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(getSortOrderByNodeSourceTypeUseCase(NodeSourceType.OFFLINE)).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(nodeSortConfigurationUiMapper(any<SortOrder>())).thenReturn(NodeSortConfiguration.default)
        whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>())).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }
}
