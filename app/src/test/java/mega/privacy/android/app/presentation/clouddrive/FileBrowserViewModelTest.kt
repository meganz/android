package mega.privacy.android.app.presentation.clouddrive

import android.view.MenuItem
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.clouddrive.mapper.StorageCapacityMapper
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsColoredFoldersOnboardingShownUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.SetColoredFoldersOnboardingShownUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.DoesUriPathExistsUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.mediadiscovery.ShouldEnterMediaDiscoveryModeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileBrowserViewModelTest {
    private lateinit var underTest: FileBrowserViewModel

    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
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
    private val containsMediaItemUseCase = mock<ContainsMediaItemUseCase>()
    private val fileDurationMapper = mock<FileDurationMapper>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val isColoredFoldersOnboardingShownUseCase = mock<IsColoredFoldersOnboardingShownUseCase>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase>() {
        onBlocking {
            invoke(NodeId(any()))
        }.thenReturn(false)
    }
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val accountDetailFakeFlow = MutableSharedFlow<AccountDetail>()
    private val shouldEnterMediaDiscoveryModeUseCase = mock<ShouldEnterMediaDiscoveryModeUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val storageCapacityMapper = mock<StorageCapacityMapper>()
    private val setAlmostFullStorageBannerClosingTimestampUseCase =
        mock<SetAlmostFullStorageBannerClosingTimestampUseCase>()
    private val setColoredFoldersOnboardingShownUseCase = mock<SetColoredFoldersOnboardingShownUseCase>()
    private val monitorAlmostFullStorageBannerClosingTimestampUseCase =
        mock<MonitorAlmostFullStorageBannerVisibilityUseCase>()
    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()
    private val doesUriPathExistsUseCase = mock<DoesUriPathExistsUseCase>()

    @BeforeEach
    fun setUp() {
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
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            getCloudSortOrder = getCloudSortOrder,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            handleOptionClickMapper = handleOptionClickMapper,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getBandwidthOverQuotaDelayUseCase = getBandwidthOverQuotaDelayUseCase,
            containsMediaItemUseCase = containsMediaItemUseCase,
            fileDurationMapper = fileDurationMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isColoredFoldersOnboardingShownUseCase = isColoredFoldersOnboardingShownUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            shouldEnterMediaDiscoveryModeUseCase = shouldEnterMediaDiscoveryModeUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
            setColoredFoldersOnboardingShownUseCase = setColoredFoldersOnboardingShownUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase = monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper = storageCapacityMapper,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            doesUriPathExistsUseCase = doesUriPathExistsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher()
        )
    }

    private fun provideStorageStateParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            StorageState.Red,
            true,
            StorageOverQuotaCapacity.FULL
        ),
        Arguments.of(
            StorageState.Green,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.Orange,
            true,
            StorageOverQuotaCapacity.ALMOST_FULL
        ), Arguments.of(
            StorageState.Orange,
            false,
            StorageOverQuotaCapacity.DEFAULT
        ),
        Arguments.of(
            StorageState.Change,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.Unknown,
            true,
            StorageOverQuotaCapacity.DEFAULT
        ), Arguments.of(
            StorageState.PayWall,
            true,
            StorageOverQuotaCapacity.DEFAULT
        )
    )


    @ParameterizedTest(name = "when storage state is: {0} and isDismissiblePeriodOver is: {1} then storageCapacity is: {2}")
    @MethodSource("provideStorageStateParameters")
    fun `test that storageCapacity is updated correctly when monitorStorageStateUseCase is invoked`(
        storageState: StorageState,
        isDismissiblePeriodOver: Boolean,
        storageOverQuotaCapacity: StorageOverQuotaCapacity,
    ) = runTest {
        runBlocking {
            stubCommon()
            whenever(monitorStorageStateUseCase()).thenReturn(
                storageState.asHotFlow()
            )
            whenever(monitorAlmostFullStorageBannerClosingTimestampUseCase()).thenReturn(
                flowOf(
                    isDismissiblePeriodOver
                )
            )
            whenever(
                storageCapacityMapper(
                    storageState = storageState,
                    shouldShow = isDismissiblePeriodOver
                )
            ).thenReturn(
                storageOverQuotaCapacity
            )
        }
        initViewModel()
        underTest.state.map { it.storageCapacity }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(storageOverQuotaCapacity)
        }
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.fileBrowserHandle).isEqualTo(-1L)
            assertThat(initial.mediaDiscoveryViewSettings)
                .isEqualTo(MediaDiscoveryViewSettings.INITIAL.ordinal)
            assertThat(initial.nodesList).isEmpty()
        }
    }

    @Test
    fun `test that the file browser handle is updated if new value provided`() = runTest {
        underTest.state.map { it.fileBrowserHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setFileBrowserHandle(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that get safe browser parent handle returns INVALID_HANDLE if not set and root folder fails`() =
        runTest {
            whenever(getRootNodeUseCase()).thenReturn(null)
            assertThat(underTest.getSafeBrowserParentHandle())
                .isEqualTo(MegaApiJava.INVALID_HANDLE)
        }

    @Test
    fun `test that the file browser handle is set`() =
        runTest {
            val expectedHandle = 123456789L
            underTest.setFileBrowserHandle(expectedHandle)
            assertThat(underTest.getSafeBrowserParentHandle()).isEqualTo(expectedHandle)
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
            assertThat(underTest.state.value.nodesList.size).isEqualTo(2)
        }

    @Test
    fun `test that no nodes are returned when setting the file browser handle and the file browser node is null`() =
        runTest {
            val newValue = 123456789L
            whenever(getFileBrowserNodeChildrenUseCase.invoke(newValue)).thenReturn(emptyList())
            underTest.setFileBrowserHandle(newValue)
            assertThat(underTest.state.value.nodesList.size).isEqualTo(0)
            verify(getFileBrowserNodeChildrenUseCase).invoke(newValue)
        }

    @Test
    fun `test that media discovery cannot be entered when shouldEnterMediaDiscoveryModeUseCase is false and setting is in initial state`() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(false)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.INITIAL.ordinal
                )
            assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that media discovery cannot be entered when shouldEnterMediaDiscoveryModeUseCase is false and setting is in enabled state`() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(false)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that media discovery cannot be entered when shouldEnterMediaDiscoveryModeUseCase is false and setting is in disabled state`() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(false)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.DISABLED.ordinal
                )
            assertThat(shouldEnter).isFalse()
        }

    @Test
    fun `test that media discovery can be entered when shouldEnterMediaDiscoveryModeUseCase is true and setting is in initial state `() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(true)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.INITIAL.ordinal
                )
            assertThat(shouldEnter).isTrue()
        }

    @Test
    fun `test that media discovery can be entered when shouldEnterMediaDiscoveryModeUseCase is true and setting is in enabled state `() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(true)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            assertThat(shouldEnter).isTrue()
        }

    @Test
    fun `test that media discovery cannot be entered when shouldEnterMediaDiscoveryModeUseCase is true and setting is in disabled state `() =
        runTest {
            val newValue = 123456789L
            whenever(shouldEnterMediaDiscoveryModeUseCase(newValue)).thenReturn(true)

            val shouldEnter =
                underTest.shouldEnterMediaDiscoveryMode(
                    newValue,
                    MediaDiscoveryViewSettings.DISABLED.ordinal
                )
            assertThat(shouldEnter).isFalse()
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
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodeHandles.size).isEqualTo(1)
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
                assertThat(state.selectedFolderNodes).isEqualTo(0)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodeHandles.size).isEqualTo(0)
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
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(1)
                assertThat(state.selectedNodeHandles.size).isEqualTo(2)
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
            assertThat(underTest.state.value.nodesList.size)
                .isEqualTo(underTest.state.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that the selected node handles is empty when clearing all nodes`() = runTest {
        underTest.clearAllNodes()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedNodeHandles).isEmpty()
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
            assertThat(awaitItem().isPendingRefresh).isFalse()
            flow.emit(Unit)
            assertThat(awaitItem().isPendingRefresh).isTrue()
        }
    }

    @Test
    fun `test that the transfer overquota banner is hidden when transfers management detects a non overquota state`() =
        runTest {
            whenever(isInTransferOverQuotaUseCase()).thenReturn(false)
            whenever(getBandwidthOverQuotaDelayUseCase()).thenReturn(10000.seconds)
            underTest.changeTransferOverQuotaBannerVisibility()
            underTest.state.test {
                assertThat(awaitItem().shouldShowBannerVisibility).isFalse()
            }
        }

    @Test
    fun `test that download event is updated when on download option click is invoked and both download option and feature flag are true`() =
        runTest {
            onDownloadOptionClick()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            }
        }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that download event is updated when on available offline option click is invoked`(
        withStartMessage: Boolean,
    ) = runTest {
        val triggered = TransferTriggerEvent.StartDownloadForOffline(
            node = mock(),
            withStartMessage = withStartMessage
        )
        underTest.onDownloadFileTriggered(triggered)
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                .isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            assertThat(((state.downloadEvent as StateEventWithContentTriggered).content as TransferTriggerEvent.StartDownloadForOffline).withStartMessage)
                .isEqualTo(withStartMessage)
        }
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that download event is updated when on download option click is invoked`(
        withStartMessage: Boolean,
    ) = runTest {
        val triggered = TransferTriggerEvent.StartDownloadNode(
            nodes = listOf(mock()),
            withStartMessage = withStartMessage
        )
        underTest.onDownloadFileTriggered(triggered)
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            assertThat(((state.downloadEvent as StateEventWithContentTriggered).content as TransferTriggerEvent.StartDownloadNode).withStartMessage)
                .isEqualTo(withStartMessage)
        }
    }

    @Test
    fun `test that download event is updated when on download for preview option click is invoked`() =
        runTest {
            val triggered =
                TransferTriggerEvent.StartDownloadForPreview(node = mock(), isOpenWith = false)
            underTest.onDownloadFileTriggered(triggered)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadForPreview::class.java)
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
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    private suspend fun onDownloadOptionClick() {
        val menuItem = mock<MenuItem>()
        val optionsItemInfo =
            OptionsItemInfo(OptionItems.DOWNLOAD_CLICKED, emptyList(), emptyList())
        whenever(handleOptionClickMapper(eq(menuItem), any())).thenReturn(optionsItemInfo)
        underTest.onOptionItemClicked(menuItem)
    }

    @Test
    fun `test that when folder is selected it calls update handle`() = runTest {
        val handle = 123456L
        underTest.onFolderItemClicked(handle)
        underTest.state.test {
            assertThat(awaitItem().isLoading).isFalse()
        }
    }

    @Test
    fun `test that sensitive nodes should be filtered based on account setting`() = runTest {
        // given
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.BUSINESS,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(accountDetail)

        val nodesListItem1 = mock<TypedFileNode>()
        whenever(nodesListItem1.id.longValue).thenReturn(1L)
        whenever(nodesListItem1.isMarkedSensitive).thenReturn(false)
        whenever(nodesListItem1.isSensitiveInherited).thenReturn(false)

        val nodesListItem2 = mock<TypedFolderNode>()
        whenever(nodesListItem2.id.longValue).thenReturn(2L)
        whenever(nodesListItem2.isMarkedSensitive).thenReturn(true)
        whenever(nodesListItem2.isSensitiveInherited).thenReturn(true)

        whenever(getFileBrowserNodeChildrenUseCase(underTest.state.value.fileBrowserHandle))
            .thenReturn(listOf(nodesListItem1, nodesListItem2))
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

        // when
        underTest.refreshNodes()

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.sourceNodesList).hasSize(2)
            assertThat(state.nodesList).hasSize(1)
        }
    }

    @Test
    fun `test openFileBrowserWithSpecificNode updates state and refreshes nodes`() = runTest {
        val folderHandle = 123456789L

        // Mock the necessary dependencies
        whenever(shouldEnterMediaDiscoveryModeUseCase(folderHandle)).thenReturn(false)

        // Call the function
        underTest.openFileBrowserWithSpecificNode(folderHandle, null)

        // Verify the state updates
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.fileBrowserHandle).isEqualTo(folderHandle)
            assertThat(state.accessedFolderHandle).isEqualTo(folderHandle)
            assertThat(state.openedFolderNodeHandles).isEmpty()
            assertThat(state.errorMessage).isEqualTo(null)
            assertThat(state.selectedTab).isEqualTo(CloudDriveTab.CLOUD)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify that refreshNodesState is called
        verify(getFileBrowserNodeChildrenUseCase).invoke(folderHandle)
    }

    @Test
    fun `test colored folders onboarding is shown when not previously shown`() = runTest {
        // given
        whenever(isColoredFoldersOnboardingShownUseCase()).thenReturn(false)

        // when
        underTest = FileBrowserViewModel(
            getRootNodeUseCase = getRootNodeUseCase,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            getCloudSortOrder = getCloudSortOrder,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            handleOptionClickMapper = handleOptionClickMapper,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getBandwidthOverQuotaDelayUseCase = getBandwidthOverQuotaDelayUseCase,
            containsMediaItemUseCase = containsMediaItemUseCase,
            fileDurationMapper = fileDurationMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isColoredFoldersOnboardingShownUseCase = isColoredFoldersOnboardingShownUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            shouldEnterMediaDiscoveryModeUseCase = shouldEnterMediaDiscoveryModeUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
            setColoredFoldersOnboardingShownUseCase = setColoredFoldersOnboardingShownUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase = monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper = storageCapacityMapper,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            doesUriPathExistsUseCase = doesUriPathExistsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher()
        )

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showColoredFoldersOnboarding).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test colored folders onboarding is not shown when previously shown`() = runTest {
        // given
        whenever(isColoredFoldersOnboardingShownUseCase()).thenReturn(true)

        // when
        underTest = FileBrowserViewModel(
            getRootNodeUseCase = getRootNodeUseCase,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            getCloudSortOrder = getCloudSortOrder,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            handleOptionClickMapper = handleOptionClickMapper,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getBandwidthOverQuotaDelayUseCase = getBandwidthOverQuotaDelayUseCase,
            containsMediaItemUseCase = containsMediaItemUseCase,
            fileDurationMapper = fileDurationMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isColoredFoldersOnboardingShownUseCase = isColoredFoldersOnboardingShownUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            shouldEnterMediaDiscoveryModeUseCase = shouldEnterMediaDiscoveryModeUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
            setColoredFoldersOnboardingShownUseCase = setColoredFoldersOnboardingShownUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase = monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper = storageCapacityMapper,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            doesUriPathExistsUseCase = doesUriPathExistsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher()
        )

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showColoredFoldersOnboarding).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test onColoredFoldersOnboardingDismissed marks onboarding as shown and updates state`() = runTest {
        // given
        whenever(setColoredFoldersOnboardingShownUseCase()).thenReturn(Unit)

        // when
        underTest.onColoredFoldersOnboardingDismissed()

        // then
        verify(setColoredFoldersOnboardingShownUseCase).invoke()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showColoredFoldersOnboarding).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(emptyList())
        whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(null)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorRefreshSessionUseCase()).thenReturn(emptyFlow())
        whenever(getBandwidthOverQuotaDelayUseCase()).thenReturn(1.seconds)
        whenever(fileDurationMapper(any())).thenReturn(1.seconds)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFakeFlow)
        whenever(shouldEnterMediaDiscoveryModeUseCase(any())).thenReturn(false)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorStorageStateUseCase()).thenReturn(
            StorageState.Green.asHotFlow()
        )
        whenever(isColoredFoldersOnboardingShownUseCase()).thenReturn(false)
        whenever(setColoredFoldersOnboardingShownUseCase()).thenReturn(Unit)
        whenever(setAlmostFullStorageBannerClosingTimestampUseCase()).thenReturn(Unit)
        whenever(monitorAlmostFullStorageBannerClosingTimestampUseCase()).thenReturn(flowOf(true))
        whenever(
            storageCapacityMapper(
                storageState = any(),
                shouldShow = any()
            )
        ).thenReturn(
            StorageOverQuotaCapacity.DEFAULT
        )
        whenever(isInTransferOverQuotaUseCase()).thenReturn(false)
    }

    @AfterEach
    fun resetMocks() {
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
            containsMediaItemUseCase,
            fileDurationMapper,
            monitorOfflineNodeUpdatesUseCase,
            monitorConnectivityUseCase,
            monitorStorageStateUseCase,
            getFeatureFlagValueUseCase,
            isColoredFoldersOnboardingShownUseCase,
            setColoredFoldersOnboardingShownUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase,
            monitorAlmostFullStorageBannerClosingTimestampUseCase,
            storageCapacityMapper,
            doesUriPathExistsUseCase,
        )
    }
}
