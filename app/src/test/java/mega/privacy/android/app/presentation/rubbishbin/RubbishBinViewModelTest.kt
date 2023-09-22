package mega.privacy.android.app.presentation.rubbishbin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolderUseCase
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.GetIntentToOpenFileMapper
import mega.privacy.android.app.presentation.rubbishbin.model.RestoreType
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class RubbishBinViewModelTest {

    private lateinit var underTest: RubbishBinViewModel

    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val getRubbishBinParentNodeHandle = mock<GetParentNodeHandle>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase>()
    private val setViewType = mock<SetViewType>()
    private val monitorViewType = mock<MonitorViewType>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getIntentToOpenFileMapper = mock<GetIntentToOpenFileMapper>()
    private val getRubbishBinFolderUseCase = mock<GetRubbishBinFolderUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getRubbishBinChildren = mock<GetRubbishBinChildren>()
    private val fileDurationMapper: FileDurationMapper = mock {
        onBlocking { invoke(any()) }.thenReturn(null)
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = RubbishBinViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            getRubbishBinParentNodeHandle = getRubbishBinParentNodeHandle,
            getRubbishBinChildren = getRubbishBinChildren,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            getCloudSortOrder = getCloudSortOrder,
            getIntentToOpenFileMapper = getIntentToOpenFileMapper,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            getNodeByHandle = getNodeByHandle,
            fileDurationMapper = fileDurationMapper
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.rubbishBinHandle).isEqualTo(-1L)
            Truth.assertThat(initial.parentHandle).isNull()
            Truth.assertThat(initial.nodeList).isEmpty()
            Truth.assertThat(initial.selectedFileNodes).isEqualTo(0)
            Truth.assertThat(initial.selectedFolderNodes).isEqualTo(0)
            Truth.assertThat(initial.isInSelection).isFalse()
            Truth.assertThat(initial.currFileNode).isNull()
            Truth.assertThat(initial.itemIndex).isEqualTo(-1)
            Truth.assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            Truth.assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            Truth.assertThat(initial.selectedNodeHandles).isEmpty()
            Truth.assertThat(initial.selectedMegaNodes).isNull()
            Truth.assertThat(initial.isPendingRefresh).isFalse()
            Truth.assertThat(initial.isRubbishBinEmpty).isFalse()
            Truth.assertThat(initial.restoreType).isNull()
        }
    }

    @Test
    fun `test that rubbish bin handle is updated if new value provided`() = runTest {
        underTest.state.map { it.rubbishBinHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                Truth.assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setRubbishBinHandle(newValue)
                Truth.assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns empty list`() =
        runTest {
            val newValue = 123456789L
            whenever(getRubbishBinChildren.invoke(newValue)).thenReturn(ArrayList())
            monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
            underTest.setRubbishBinHandle(newValue)
            Truth.assertThat(underTest.state.value.nodeList.size).isEqualTo(0)
        }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns null`() = runTest {
        val newValue = 123456789L
        whenever(getRubbishBinChildren(newValue)).thenReturn(emptyList())
        underTest.setRubbishBinHandle(newValue)
        Truth.assertThat(underTest.state.value.nodeList.size).isEqualTo(0)
        verify(getRubbishBinChildren, times(1)).invoke(newValue)
    }

    @Test
    fun `test that when handle on back pressed and parent handle is null, then getRubbishBinChildrenNode is not invoked`() =
        runTest {
            val newValue = 123456789L
            underTest.onBackPressed()
            verify(getRubbishBinChildren, times(0)).invoke(newValue)
        }

    @Test
    fun `test that when handle on back pressed and parent handle is not null, then getRubbishBinChildrenNode is invoked once`() =
        runTest {
            val newValue = 123456789L
            // to update handles rubbishBinHandle
            whenever(getRubbishBinChildren(newValue)).thenReturn(emptyList())
            underTest.setRubbishBinHandle(newValue)
            underTest.onBackPressed()
            verify(getRubbishBinChildren, times(1)).invoke(newValue)
        }

    @Test
    fun `test when when nodeUIItem is long clicked, then it updates selected item by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that when item is clicked and some items are already selected on list then checked index gets decremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(0)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that when selected item gets clicked then checked index gets incremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem2,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(1)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(2)
            }
        }

    @Test
    fun `test that on clicking on change view type to Grid it calls setViewType atleast once`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType, times(1)).invoke(ViewType.GRID)
        }

    @Test
    fun `test when user selects all nodes then sum of selected items is equal to size of list`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            val totalSelectedNodes =
                underTest.state.value.selectedFileNodes + underTest.state.value.selectedFileNodes
            Truth.assertThat(totalSelectedNodes).isEqualTo(underTest.state.value.nodeList.size)
            Truth.assertThat(totalSelectedNodes)
                .isEqualTo(underTest.state.value.selectedNodeHandles.size)
            Truth.assertThat(underTest.state.value.isInSelection).isTrue()
        }

    @Test
    fun `test when user clears all selected nodes then sum of selected items is 0`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.clearAllSelectedNodes()
            val totalSelectedNodes =
                underTest.state.value.selectedFileNodes + underTest.state.value.selectedFileNodes
            Truth.assertThat(totalSelectedNodes).isEqualTo(0)
            Truth.assertThat(totalSelectedNodes)
                .isEqualTo(0)
            Truth.assertThat(underTest.state.value.isInSelection).isFalse()
            Truth.assertThat(underTest.state.value.selectedMegaNodes).isNull()
        }

    @Test
    fun `test that when any file item is clicked and no other item is selected then it updates FileNode in state`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onItemClicked(
                NodeUIItem(
                    node = nodesListItem2,
                    isSelected = false,
                    isInvisible = false
                )
            )
            Truth.assertThat(underTest.state.value.currFileNode).isNotNull()
            Truth.assertThat(underTest.state.value.itemIndex).isNotEqualTo(-1)
        }

    @Test
    fun `test that restoring nodes will execute the move functionality when backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            val megaNode1 = mock<MegaNode>()
            val megaNode2 = mock<MegaNode>()
            whenever(megaNode1.handle).thenReturn(1L)
            whenever(megaNode2.handle).thenReturn(2L)

            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(any()))).thenReturn(true)

            whenever(getNodeByHandle(1L)).thenReturn(megaNode1)
            whenever(getNodeByHandle(2L)).thenReturn(megaNode2)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.MOVE)
            }
        }

    @Test
    fun `test that restoring nodes will execute the move functionality when backup and non backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            val megaNode1 = mock<MegaNode>()
            val megaNode2 = mock<MegaNode>()
            whenever(megaNode1.handle).thenReturn(1L)
            whenever(megaNode2.handle).thenReturn(2L)

            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(1L))).thenReturn(true)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(2L))).thenReturn(false)

            whenever(getNodeByHandle(1L)).thenReturn(megaNode1)
            whenever(getNodeByHandle(2L)).thenReturn(megaNode2)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.MOVE)
            }
        }

    @Test
    fun `test that restoring nodes will execute the restore functionality when non backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            val megaNode1 = mock<MegaNode>()
            val megaNode2 = mock<MegaNode>()
            whenever(megaNode1.handle).thenReturn(1L)
            whenever(megaNode2.handle).thenReturn(2L)
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinChildren(underTest.state.value.rubbishBinHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(any()))).thenReturn(false)
            whenever(getNodeByHandle(1L)).thenReturn(megaNode1)
            whenever(getNodeByHandle(2L)).thenReturn(megaNode2)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.RESTORE)
            }
        }

    @Test
    fun `test that acknowledging the restore functionality will reset the restore type`() =
        runTest {
            underTest.onRestoreHandled()
            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isNull()
            }
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
