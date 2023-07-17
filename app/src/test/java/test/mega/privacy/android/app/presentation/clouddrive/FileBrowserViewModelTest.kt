package test.mega.privacy.android.app.presentation.clouddrive

import android.view.MenuItem
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetBandWidthOverQuotaDelayUseCase
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetFileBrowserChildrenUseCase
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.clouddrive.model.OptionsItemInfo
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class FileBrowserViewModelTest {
    private lateinit var underTest: FileBrowserViewModel

    private val getRootFolder = mock<GetRootFolder>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val getBrowserChildrenNode = mock<GetBrowserChildrenNode>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }
    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val getFileBrowserParentNodeHandle = mock<GetParentNodeHandle>()
    private val getFileBrowserChildrenUseCase: GetFileBrowserChildrenUseCase = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getOptionsForToolbarMapper: GetOptionsForToolbarMapper = mock()
    private val handleOptionClickMapper: HandleOptionClickMapper = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val setViewType: SetViewType = mock()
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase = mock()
    private val getBandWidthOverQuotaDelayUseCase: GetBandWidthOverQuotaDelayUseCase = mock()
    private val transfersManagement: TransfersManagement = mock()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = FileBrowserViewModel(
            getRootFolder = getRootFolder,
            getBrowserChildrenNode = getBrowserChildrenNode,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            monitorNodeUpdates = monitorNodeUpdates,
            getFileBrowserParentNodeHandle = getFileBrowserParentNodeHandle,
            getIsNodeInRubbish = isNodeInRubbish,
            getFileBrowserChildrenUseCase = getFileBrowserChildrenUseCase,
            getCloudSortOrder = getCloudSortOrder,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            getOptionsForToolbarMapper = getOptionsForToolbarMapper,
            handleOptionClickMapper = handleOptionClickMapper,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getBandWidthOverQuotaDelayUseCase = getBandWidthOverQuotaDelayUseCase,
            transfersManagement = transfersManagement
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            Truth.assertThat(initial.fileBrowserHandle).isEqualTo(-1L)
            Truth.assertThat(initial.mediaDiscoveryViewSettings)
                .isEqualTo(MediaDiscoveryViewSettings.INITIAL.ordinal)
            Truth.assertThat(initial.nodes).isEmpty()
            Truth.assertThat(initial.parentHandle).isNull()
            Truth.assertThat(initial.mediaHandle).isEqualTo(-1L)
            Truth.assertThat(initial.nodesList).isEmpty()
        }
    }


    @Test
    fun `test that browser parent handle is updated if new value provided`() = runTest {
        underTest.state.map { it.fileBrowserHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                Truth.assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setBrowserParentHandle(newValue)
                Truth.assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that get safe browser handle returns INVALID_HANDLE if not set and root folder fails`() =
        runTest {
            whenever(getRootFolder()).thenReturn(null)
            Truth.assertThat(underTest.getSafeBrowserParentHandle())
                .isEqualTo(MegaApiJava.INVALID_HANDLE)
        }

    @Test
    fun `test that get safe browser handle returns if set`() =
        runTest {
            val expectedHandle = 123456789L
            underTest.setBrowserParentHandle(expectedHandle)
            Truth.assertThat(underTest.getSafeBrowserParentHandle()).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that on setting Browser Parent Handle, handle File Browser node returns some items in list`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode(newValue)).thenReturn(
                listOf(mock(), mock())
            )
            whenever(getFileBrowserChildrenUseCase(newValue)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)
            Truth.assertThat(underTest.state.value.nodes.size).isEqualTo(2)
        }

    @Test
    fun `test that on setting Browser Parent Handle, handle File Browser node returns null`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(null)
            whenever(getFileBrowserChildrenUseCase.invoke(newValue)).thenReturn(emptyList())
            underTest.setBrowserParentHandle(newValue)
            Truth.assertThat(underTest.state.value.nodes.size).isEqualTo(0)
            verify(getBrowserChildrenNode).invoke(newValue)
            verify(getFileBrowserChildrenUseCase).invoke(newValue)
        }

    @Test
    fun `test that when nodes are empty then Enter in MD mode will return false`() = runTest {
        val newValue = 123456789L
        whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(null)
        underTest.setBrowserParentHandle(newValue)

        val shouldEnter =
            underTest.shouldEnterMediaDiscoveryMode(
                newValue,
                MediaDiscoveryViewSettings.INITIAL.ordinal
            )
        Truth.assertThat(shouldEnter).isFalse()
    }

    @Test
    fun `test that when MediaDiscoveryViewSettings is Disabled then Enter in MD mode will return false`() =
        runTest {
            val newValue = 123456789L
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(
                listOf(mock(), mock())
            )
            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.DISABLED.ordinal
                )
            Truth.assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that when MediaDiscoveryViewSettings is Enabled and nodes contains not folder then Enter in MD mode will return false`() =
        runTest {
            val newValue = 123456789L
            val folderNode = mock<MegaNode> {
                on { isFolder }.thenReturn(true)
            }
            whenever(getBrowserChildrenNode.invoke(newValue)).thenReturn(listOf(folderNode))

            underTest.setBrowserParentHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            Truth.assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that when folder is clicked from adapter, then stack gets updated with appropriate value`() =
        runTest {
            val lastFirstVisiblePosition = 123456
            val newValue = 12345L

            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdates.emit(NodeUpdate(update))
            underTest.setBrowserParentHandle(newValue)

            underTest.onFolderItemClicked(lastFirstVisiblePosition, newValue)
            Truth.assertThat(underTest.popLastPositionStack()).isEqualTo(lastFirstVisiblePosition)
        }

    @Test
    fun `test that last position returns 0 when items are popped from stack and stack has no items`() {
        val poppedValue = underTest.popLastPositionStack()
        Truth.assertThat(poppedValue).isEqualTo(0)
    }

    @Test
    fun `test that when handle on back pressed and parent handle is null, then getRubbishBinChildrenNode is not invoked`() =
        runTest {
            val newValue = 123456789L
            underTest.onBackPressed()
            verify(getBrowserChildrenNode, times(0)).invoke(newValue)
        }

    @Test
    fun `test that when handle on back pressed and parent handle is not null, then getBrowserChildrenNode is invoked once`() =
        runTest {
            val newValue = 123456789L
            // to update handles fileBrowserHandle
            whenever(getFileBrowserChildrenUseCase.invoke(newValue)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            underTest.setBrowserParentHandle(newValue)
            underTest.onBackPressed()
            verify(getBrowserChildrenNode).invoke(newValue)
            verify(getFileBrowserChildrenUseCase).invoke(newValue)
        }

    @Test
    fun `test when when nodeUIItem is long clicked, then it updates selected item by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getBrowserChildrenNode(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(mock(), mock())
            )
            whenever(getFileBrowserChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem<TypedNode>(
                    nodesListItem1,
                    isSelected = true,
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
            whenever(getBrowserChildrenNode(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(mock(), mock())
            )
            whenever(getFileBrowserChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem<TypedNode>(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem<TypedNode>(
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
            whenever(getBrowserChildrenNode(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(mock(), mock())
            )
            whenever(getFileBrowserChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem<TypedNode>(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem<TypedNode>(
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
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that when select all nodes clicked size of node items and equal to size of selected nodes`() =
        runTest {
            whenever(getBrowserChildrenNode(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf(mock(), mock())
            )
            whenever(getFileBrowserChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.refreshNodes()
            underTest.selectAllNodes()
            Truth.assertThat(underTest.state.value.nodesList.size)
                .isEqualTo(underTest.state.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that when clear all nodes clicked, size of selected nodes is empty`() = runTest {
        underTest.clearAllNodes()
        Truth.assertThat(underTest.state.value.selectedNodeHandles).isEmpty()
    }

    @Test
    fun `test that when prepare options menu clicked it invokes getOptionsForToolbarMapper`() =
        runTest {
            underTest.prepareForGetOptionsForToolbar()
            verify(getOptionsForToolbarMapper).invoke(emptyList(), 0)
        }

    @Test
    fun `test that when onOptionItemClicked then it invokes handleOptionClickMapper`() = runTest {
        val menuItem: MenuItem = mock()
        whenever(handleOptionClickMapper.invoke(menuItem, emptyList())).thenReturn(
            OptionsItemInfo(
                optionClickedType = OptionItems.MOVE_CLICKED,
                selectedNode = emptyList(),
                selectedMegaNode = emptyList()
            )
        )
        underTest.onOptionItemClicked(item = menuItem)
        verify(handleOptionClickMapper).invoke(menuItem, emptyList())
    }

    @Test
    fun `test that isPendingRefresh as true when monitorRefreshSessionUseCase emit`() = runTest {
        val flow = MutableSharedFlow<Unit>()
        whenever(monitorRefreshSessionUseCase()).thenReturn(flow)
        initViewModel()
        advanceUntilIdle()
        underTest.state.test {
            Truth.assertThat(awaitItem().isPendingRefresh).isFalse()
            flow.emit(Unit)
            Truth.assertThat(awaitItem().isPendingRefresh).isTrue()
        }
    }

    @Test
    fun `test that when transfer management quota is false then visibility for transfer in state is false`() =
        runTest {
            whenever(transfersManagement.isTransferOverQuotaBannerShown).thenReturn(false)
            whenever(getBandWidthOverQuotaDelayUseCase()).thenReturn(10000)
            underTest.changeTransferOverQuotaBannerVisibility()
            underTest.state.test {
                Truth.assertThat(awaitItem().shouldShowBannerVisibility).isFalse()
            }
        }
}
