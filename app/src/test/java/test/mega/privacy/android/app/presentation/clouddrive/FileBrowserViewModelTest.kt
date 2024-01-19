package test.mega.privacy.android.app.presentation.clouddrive

import android.view.MenuItem
import app.cash.turbine.test
import com.google.common.truth.Truth
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.clouddrive.FileBrowserViewModel
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.quota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileBrowserViewModelTest {
    private lateinit var underTest: FileBrowserViewModel

    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val getParentNodeUseCase = mock<GetParentNodeUseCase>()
    private val getFileBrowserNodeChildrenUseCase = mock<GetFileBrowserNodeChildrenUseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val handleOptionClickMapper = mock<HandleOptionClickMapper>()
    private val monitorViewType = mock<MonitorViewType>()
    private val setViewType = mock<SetViewType>()
    private val monitorRefreshSessionUseCase = mock<MonitorRefreshSessionUseCase>()
    private val getBandwidthOverQuotaDelayUseCase = mock<GetBandwidthOverQuotaDelayUseCase>()
    private val transfersManagement = mock<TransfersManagement>()
    private val containsMediaItemUseCase = mock<ContainsMediaItemUseCase>()
    private val fileDurationMapper = mock<FileDurationMapper>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = FileBrowserViewModel(
            getRootNodeUseCase = getRootNodeUseCase,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
            getIsNodeInRubbish = isNodeInRubbish,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            getCloudSortOrder = getCloudSortOrder,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            handleOptionClickMapper = handleOptionClickMapper,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getBandwidthOverQuotaDelayUseCase = getBandwidthOverQuotaDelayUseCase,
            transfersManagement = transfersManagement,
            containsMediaItemUseCase = containsMediaItemUseCase,
            fileDurationMapper = fileDurationMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase
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
            Truth.assertThat(initial.parentHandle).isNull()
            Truth.assertThat(initial.nodesList).isEmpty()
        }
    }


    @Test
    fun `test that the file browser handle is updated if new value provided`() = runTest {
        underTest.state.map { it.fileBrowserHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                Truth.assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setFileBrowserHandle(newValue)
                Truth.assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that get safe browser parent handle returns INVALID_HANDLE if not set and root folder fails`() =
        runTest {
            whenever(getRootNodeUseCase()).thenReturn(null)
            Truth.assertThat(underTest.getSafeBrowserParentHandle())
                .isEqualTo(MegaApiJava.INVALID_HANDLE)
        }

    @Test
    fun `test that the file browser handle is set`() =
        runTest {
            val expectedHandle = 123456789L
            underTest.setFileBrowserHandle(expectedHandle)
            Truth.assertThat(underTest.getSafeBrowserParentHandle()).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that the nodes are returned when setting the file browser handle`() =
        runTest {
            val newValue = 123456789L
            whenever(getFileBrowserNodeChildrenUseCase(newValue)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            val update = mapOf<Node, List<NodeChanges>>(
                mock<Node>() to emptyList(),
                mock<Node>() to emptyList()
            )
            monitorNodeUpdatesFakeFlow.emit(NodeUpdate(update))
            underTest.setFileBrowserHandle(newValue)
            Truth.assertThat(underTest.state.value.nodesList.size).isEqualTo(2)
        }

    @Test
    fun `test that no nodes are returned when setting the file browser handle and the file browser node is null`() =
        runTest {
            val newValue = 123456789L
            whenever(getFileBrowserNodeChildrenUseCase.invoke(newValue)).thenReturn(emptyList())
            underTest.setFileBrowserHandle(newValue)
            Truth.assertThat(underTest.state.value.nodesList.size).isEqualTo(0)
            verify(getFileBrowserNodeChildrenUseCase).invoke(newValue)
        }

    @Test
    fun `test that media discovery cannot be entered when there are no nodes`() = runTest {
        val newValue = 123456789L
        whenever(getFileBrowserNodeChildrenUseCase.invoke(newValue)).thenReturn(emptyList())
        underTest.setFileBrowserHandle(newValue)

        val shouldEnter =
            underTest.shouldEnterMediaDiscoveryMode(
                newValue,
                MediaDiscoveryViewSettings.INITIAL.ordinal
            )
        Truth.assertThat(shouldEnter).isFalse()
    }

    @Test
    fun `test that media discovery cannot be entered when media discovery view settings is disabled`() =
        runTest {
            val newValue = 123456789L
            val list = listOf<TypedFileNode>(mock(), mock())
            whenever(getFileBrowserNodeChildrenUseCase(newValue)).thenReturn(list)
            underTest.setFileBrowserHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.DISABLED.ordinal
                )
            Truth.assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that media discovery cannot be entered when media discovery view settings is enabled and a folder node is found`() =
        runTest {
            val newValue = 123456789L
            val folderNode = mock<TypedFolderNode>()
            whenever(getFileBrowserNodeChildrenUseCase.invoke(newValue)).thenReturn(
                listOf(
                    folderNode
                )
            )

            underTest.setFileBrowserHandle(newValue)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            Truth.assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that the nodes are not retrieved when a back navigation is performed and the parent handle is null`() =
        runTest {
            val newValue = 123456789L
            underTest.performBackNavigation()
            verify(getFileBrowserNodeChildrenUseCase, times(0)).invoke(newValue)
        }

    @Test
    fun `test that get file browser node children use case is invoked when a back navigation is performed and the parent handle exists`() =
        runTest {
            val newValue = 123456789L
            // to update handles fileBrowserHandle
            whenever(getFileBrowserNodeChildrenUseCase.invoke(newValue)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            underTest.setFileBrowserHandle(newValue)
            underTest.performBackNavigation()
            verify(getFileBrowserNodeChildrenUseCase).invoke(newValue)
        }

    @Test
    fun `test that the selected node handle count is incremented when a node is long clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getFileBrowserNodeChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
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
    fun `test that the selected node handle count is decremented when one of the selected nodes is clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getFileBrowserNodeChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
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
    fun `test that the selected node handle count is incremented when the selected node is clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            whenever(getFileBrowserNodeChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
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
    fun `test that set view type is called when changing the view type`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that the sizes of both selected node handles and the nodes are equal when selecting all nodes`() =
        runTest {
            whenever(getFileBrowserNodeChildrenUseCase(underTest.state.value.fileBrowserHandle)).thenReturn(
                listOf<TypedFolderNode>(mock(), mock())
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.refreshNodes()
            underTest.selectAllNodes()
            Truth.assertThat(underTest.state.value.nodesList.size)
                .isEqualTo(underTest.state.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that the selected node handles is empty when clearing all nodes`() = runTest {
        underTest.clearAllNodes()
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.selectedNodeHandles).isEmpty()
        }
    }

    @Test
    fun `test that handle option click mapper is invoked when selecting an option item`() =
        runTest {
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
    fun `test that is pending refresh is true when a node refresh event is emitted`() = runTest {
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
    fun `test that the transfer overquota banner is hidden when transfers management detects a non overquota state`() =
        runTest {
            whenever(transfersManagement.isTransferOverQuotaBannerShown).thenReturn(false)
            whenever(getBandwidthOverQuotaDelayUseCase()).thenReturn(10000)
            underTest.changeTransferOverQuotaBannerVisibility()
            underTest.state.test {
                Truth.assertThat(awaitItem().shouldShowBannerVisibility).isFalse()
            }
        }

    @Test
    fun `test that download event is updated when on download option click is invoked and both download option and feature flag are true`() =
        runTest {
            onDownloadOptionClick()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.downloadEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                Truth.assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            }
        }

    @Test
    fun `test that download event is cleared when the download event is consumed`() =
        runTest {
            //first set to triggered
            onDownloadOptionClick()
            //now we can test consume clears the state
            underTest.consumeDownloadEvent()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.downloadEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    private suspend fun onDownloadOptionClick() {
        val menuItem = mock<MenuItem>()
        val optionsItemInfo =
            OptionsItemInfo(OptionItems.DOWNLOAD_CLICKED, emptyList(), emptyList())
        whenever(handleOptionClickMapper(eq(menuItem), any())).thenReturn(optionsItemInfo)
        whenever(getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)).thenReturn(true)
        underTest.onOptionItemClicked(menuItem)
    }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(emptyList())
        whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(null)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorRefreshSessionUseCase()).thenReturn(emptyFlow())
        whenever(getBandwidthOverQuotaDelayUseCase()).thenReturn(1L)
        whenever(fileDurationMapper(any())).thenReturn(1)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
    }

    @AfterEach
    fun resetMocks() {
        Dispatchers.resetMain()
        reset(
            monitorNodeUpdatesUseCase,
            getParentNodeUseCase,
            getFileBrowserNodeChildrenUseCase,
            getCloudSortOrder,
            handleOptionClickMapper,
            monitorViewType,
            setViewType,
            monitorRefreshSessionUseCase,
            getBandwidthOverQuotaDelayUseCase,
            transfersManagement,
            containsMediaItemUseCase,
            fileDurationMapper,
            getFeatureFlagValueUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase,
        )
    }
}
