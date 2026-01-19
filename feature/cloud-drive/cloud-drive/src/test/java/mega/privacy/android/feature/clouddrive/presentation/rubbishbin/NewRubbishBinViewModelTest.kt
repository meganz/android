package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CleanRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewRubbishBinViewModelTest {

    private lateinit var underTest: NewRubbishBinViewModel

    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val getParentNodeUseCase = mock<GetParentNodeUseCase>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase>()
    private val setViewType = mock<SetViewType>()
    private val monitorViewType = mock<MonitorViewType>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val setCloudSortOrder = mock<SetCloudSortOrder>()
    private val getRubbishBinFolderUseCase = mock<GetRubbishBinFolderUseCase>()
    private val getRubbishBinNodeChildrenUseCase = mock<GetRubbishBinNodeChildrenUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val accountDetailFakeFlow = MutableSharedFlow<AccountDetail>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val cleanRubbishBinUseCase = mock<CleanRubbishBinUseCase>()
    private val getNodesByIdInChunkUseCase = mock<GetNodesByIdInChunkUseCase>()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    @AfterEach
    fun reset() {
        reset(
            getParentNodeUseCase,
            isNodeDeletedFromBackupsUseCase,
            setViewType,
            getCloudSortOrder,
            setCloudSortOrder,
            getRubbishBinFolderUseCase,
            getRubbishBinNodeChildrenUseCase,
            getBusinessStatusUseCase,
            isConnectedToInternetUseCase,
            getNodeByIdUseCase,
            nodeUiItemMapper,
            nodeSortConfigurationUiMapper,
            getFeatureFlagValueUseCase
        )
    }

    private fun initViewModel() {
        underTest = NewRubbishBinViewModel(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
            getRubbishBinNodeChildrenUseCase = getRubbishBinNodeChildrenUseCase,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            getCloudSortOrder = getCloudSortOrder,
            setCloudSortOrder = setCloudSortOrder,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            cleanRubbishBinUseCase = cleanRubbishBinUseCase,
            navKey = RubbishBinNavKey(null),
            getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.currentFolderId.longValue).isEqualTo(-1L)
            assertThat(initial.parentFolderId).isNull()
            assertThat(initial.items).isEmpty()
            assertThat(initial.selectedFileNodes).isEqualTo(0)
            assertThat(initial.selectedFolderNodes).isEqualTo(0)
            assertThat(initial.isInSelectionMode).isFalse()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.sortConfiguration).isEqualTo(NodeSortConfiguration.default)
            assertThat(initial.accountType).isNull()
            assertThat(initial.isBusinessAccountExpired).isFalse()
            assertThat(initial.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that rubbish bin handle is updated if new value provided`() = runTest {
        underTest.uiState.map { it.currentFolderId.longValue }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setRubbishBinHandle(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns empty list`() =
        runTest {
            val newValue = 123456789L
            whenever(getRubbishBinNodeChildrenUseCase.invoke(newValue)).thenReturn(ArrayList())
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(emptyList())
            monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
            underTest.setRubbishBinHandle(newValue)
            assertThat(underTest.uiState.value.items.size).isEqualTo(0)
        }

    @Test
    fun `test when when nodeUiItem is long clicked, then it updates selected item by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.uiState.value.currentFolderId.longValue)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            val nodeUiItems: List<NodeUiItem<TypedNode>> = listOf(
                NodeUiItem(node = nodesListItem1, isSelected = false),
                NodeUiItem(node = nodesListItem2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(nodeUiItems)
            underTest.refreshNodes()
            underTest.onItemLongClicked(
                NodeUiItem(
                    node = nodesListItem1,
                    isSelected = false,
                )
            )
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodes.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that when item is clicked and some items are already selected on list then checked index gets decremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.uiState.value.currentFolderId.longValue)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            val nodeUiItems: List<NodeUiItem<TypedNode>> = listOf(
                NodeUiItem(node = nodesListItem1, isSelected = false),
                NodeUiItem(node = nodesListItem2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(nodeUiItems)

            underTest.refreshNodes()
            underTest.onItemLongClicked(
                NodeUiItem(
                    node = nodesListItem1,
                    isSelected = false,
                )
            )
            underTest.onItemClicked(
                NodeUiItem(
                    node = nodesListItem1,
                    isSelected = true,
                )
            )
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.selectedFolderNodes).isEqualTo(0)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that when selected item gets clicked then checked index gets incremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.uiState.value.currentFolderId.longValue)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            val nodeUiItems: List<NodeUiItem<TypedNode>> = listOf(
                NodeUiItem(node = nodesListItem1, isSelected = false),
                NodeUiItem(node = nodesListItem2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(nodeUiItems)

            underTest.refreshNodes()
            underTest.onItemLongClicked(
                NodeUiItem(
                    node = nodesListItem1,
                    isSelected = false,
                )
            )
            underTest.onItemClicked(
                NodeUiItem(
                    node = nodesListItem2,
                    isSelected = false,
                )
            )
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(1)
                assertThat(state.selectedNodes.size).isEqualTo(2)
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
            whenever(getRubbishBinNodeChildrenUseCase(underTest.uiState.value.currentFolderId.longValue)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            val nodeUiItems: List<NodeUiItem<TypedNode>> = listOf(
                NodeUiItem(node = nodesListItem1, isSelected = false),
                NodeUiItem(node = nodesListItem2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(nodeUiItems)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            val totalSelectedNodes =
                underTest.uiState.value.selectedFileNodes + underTest.uiState.value.selectedFolderNodes
            assertThat(totalSelectedNodes).isEqualTo(underTest.uiState.value.items.size)
            assertThat(totalSelectedNodes)
                .isEqualTo(underTest.uiState.value.selectedNodes.size)
            assertThat(underTest.uiState.value.isInSelectionMode).isTrue()
        }

    @Test
    fun `test when user clears all selected nodes then sum of selected items is 0`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.uiState.value.currentFolderId.longValue)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            val nodeUiItems: List<NodeUiItem<TypedNode>> = listOf(
                NodeUiItem(node = nodesListItem1, isSelected = false),
                NodeUiItem(node = nodesListItem2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(nodeUiItems)

            underTest.refreshNodes()
            underTest.clearAllSelectedNodes()
            val totalSelectedNodes =
                underTest.uiState.value.selectedFileNodes + underTest.uiState.value.selectedFolderNodes
            assertThat(totalSelectedNodes).isEqualTo(0)
            assertThat(totalSelectedNodes)
                .isEqualTo(0)
            assertThat(underTest.uiState.value.isInSelectionMode).isFalse()
            assertThat(underTest.uiState.value.selectedNodes).isEmpty()
        }

    @Test
    fun `test that when folder is selected it calls update handle`() = runTest {
        val handle = 123456L
        whenever(
            nodeUiItemMapper(
                nodeList = any(),
                existingItems = any(),
                nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                isPublicNodes = eq(false),
                showPublicLinkCreationTime = eq(false),
                highlightedNodeId = eq(null),
                highlightedNames = eq(null),
                isContactVerificationOn = eq(false),
            )
        ).thenReturn(emptyList())
        underTest.onFolderItemClicked(NodeId(handle))
        underTest.uiState.test {
            assertThat((awaitItem().openFolderEvent as StateEventWithContentTriggered).content).isEqualTo(
                NodeId(handle)
            )
        }
    }

    @Test
    fun `test that account type is updated when monitorAccountDetailUseCase emits`() = runTest {
        stubCommon()
        initViewModel()
        val newAccountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.PRO_I,
                subscriptionStatus = null,
                subscriptionRenewTime = 0,
                accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(newAccountDetail)

        underTest.uiState.test {
            assertThat(awaitItem().accountType)
                .isEqualTo(newAccountDetail.levelDetail?.accountType)
        }
    }

    @Test
    fun `test that setSortConfiguration updates sort configuration and refreshes nodes`() =
        runTest {
            val newSortConfig = NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending)
            whenever(nodeSortConfigurationUiMapper.invoke(newSortConfig)).thenReturn(SortOrder.ORDER_DEFAULT_DESC)
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = any(),
                    nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                    isPublicNodes = eq(false),
                    showPublicLinkCreationTime = eq(false),
                    highlightedNodeId = eq(null),
                    highlightedNames = eq(null),
                    isContactVerificationOn = eq(false),
                )
            ).thenReturn(emptyList())

            underTest.setSortConfiguration(newSortConfig)

            verify(setCloudSortOrder).invoke(SortOrder.ORDER_DEFAULT_DESC)
            underTest.uiState.test {
                assertThat(awaitItem().sortConfiguration).isEqualTo(newSortConfig)
            }
        }

    @Test
    fun `test that onOpenedFileNodeHandled clears openedFileNode`() = runTest {
        val fileNode = mock<TypedFileNode>()
        underTest.uiState.value.copy(openedFileNode = fileNode)

        underTest.onOpenedFileNodeHandled()

        underTest.uiState.test {
            assertThat(awaitItem().openedFileNode).isNull()
        }
    }

    @Test
    fun `test that isConnected returns connection status`() = runTest {
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        assertThat(underTest.isConnected).isTrue()

        whenever(isConnectedToInternetUseCase()).thenReturn(false)
        assertThat(underTest.isConnected).isFalse()
    }

    @Test
    fun `test that title is updated when current node name is available`() = runTest {
        val nodeName = "Test Folder"
        val mockNode = mock<TypedNode>()
        whenever(mockNode.name).thenReturn(nodeName)
        whenever(getNodeByIdUseCase(any())).thenReturn(mockNode)
        whenever(
            nodeUiItemMapper(
                nodeList = any(),
                existingItems = any(),
                nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                isPublicNodes = eq(false),
                showPublicLinkCreationTime = eq(false),
                highlightedNodeId = eq(null),
                highlightedNames = eq(null),
                isContactVerificationOn = eq(false),
            )
        ).thenReturn(emptyList())

        underTest.refreshNodes()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.title).isEqualTo(LocalizedText.Literal(nodeName))
        }
    }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getRubbishBinNodeChildrenUseCase(any())).thenReturn(emptyList())
        whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(null)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getRubbishBinFolderUseCase()).thenReturn(null)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFakeFlow)
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(getNodeByIdUseCase(any())).thenReturn(null)
        whenever(
            nodeUiItemMapper(
                nodeList = any(),
                existingItems = any(),
                nodeSourceType = eq(NodeSourceType.RUBBISH_BIN),
                isPublicNodes = eq(false),
                showPublicLinkCreationTime = eq(false),
                highlightedNodeId = eq(null),
                highlightedNames = eq(null),
                isContactVerificationOn = eq(false),
            )
        ).thenReturn(emptyList())
        whenever(nodeSortConfigurationUiMapper.invoke(SortOrder.ORDER_NONE)).thenReturn(
            NodeSortConfiguration.default
        )
    }

    @Test
    fun `test that isSearchRevampEnabled is updated when feature flag is enabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SearchRevamp)).thenReturn(true)
        stubCommon()

        initViewModel()
        testScheduler.advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isSearchRevampEnabled).isTrue()
        }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorNodeUpdatesUseCase,
            getParentNodeUseCase,
            isNodeDeletedFromBackupsUseCase,
            setViewType,
            monitorViewType,
            getCloudSortOrder,
            setCloudSortOrder,
            getRubbishBinFolderUseCase,
            getRubbishBinNodeChildrenUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
            isConnectedToInternetUseCase,
            getNodeByIdUseCase,
            nodeUiItemMapper,
            nodeSortConfigurationUiMapper,
        )
    }
}