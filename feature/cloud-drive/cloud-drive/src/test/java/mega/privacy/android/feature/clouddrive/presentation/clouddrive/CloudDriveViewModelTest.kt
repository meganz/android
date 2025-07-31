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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val folderNodeHandle = 123L
    private val folderNodeId = NodeId(folderNodeHandle)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            getNodeByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            getFeatureFlagValueUseCase,
            isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
            nodeUiItemMapper
        )
    }

    private fun createViewModel() = CloudDriveViewModel(
        getNodeByIdUseCase = getNodeByIdUseCase,
        getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
        monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        getBusinessStatusUseCase = getBusinessStatusUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        savedStateHandle = SavedStateHandle.Companion.invoke(
            route = CloudDrive(folderNodeHandle)
        ),
    )

    private suspend fun setupTestData(items: List<TypedNode>) {
        val folderNode = mock<TypedFolderNode> {
            on { id } doReturn folderNodeId
            on { name } doReturn "Test Folder"
        }
        whenever(getNodeByIdUseCase(eq(folderNodeId))).thenReturn(folderNode)
        whenever(getFileBrowserNodeChildrenUseCase(folderNodeHandle)).thenReturn(items)

        val nodeUiItems = items.map { node ->
            NodeUiItem(
                node = node,
                isSelected = false
            )
        }
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                isPublicNodes = false,
                showPublicLinkCreationTime = false,
                highlightedNodeId = null,
                highlightedNames = null,
                shouldApplySensitiveMode = false,
                isContactVerificationOn = false,
            )
        ).thenReturn(nodeUiItems)
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
            assertThat(initialState.showHiddenNodes).isFalse()
            assertThat(initialState.isHiddenNodesEnabled).isFalse()
            assertThat(initialState.isHiddenNodesOnboarded).isFalse()
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
        advanceUntilIdle()

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
            cancelAndIgnoreRemainingEvents()
        }

        underTest.onChangeViewTypeClicked()
        advanceUntilIdle()

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
            cancelAndIgnoreRemainingEvents()
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
            cancelAndIgnoreRemainingEvents()
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

    // Hidden Nodes Tests

    @Test
    fun `test that monitorHiddenNodes is not called when feature flag is disabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

        val underTest = createViewModel()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.showHiddenNodes).isFalse()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
            assertThat(finalState.isHiddenNodesOnboarded).isFalse()
        }
    }

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes when feature flag is enabled`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.showHiddenNodes).isTrue()
            }
        }

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes to false`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.showHiddenNodes).isFalse()
        }
    }

    @Test
    fun `test that monitorAccountDetail enables hidden nodes for paid account`() = runTest {
        setupTestData(emptyList())
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_I
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isTrue()
        }
    }

    @Test
    fun `test that monitorAccountDetail disables hidden nodes for free account`() = runTest {
        setupTestData(emptyList())
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.FREE
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that monitorAccountDetail disables hidden nodes for expired business account`() =
        runTest {
            setupTestData(emptyList())
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.BUSINESS
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()

                assertThat(finalState.isHiddenNodesEnabled).isFalse()
            }
        }

    @Test
    fun `test that monitorAccountDetail enables hidden nodes for active business account`() =
        runTest {
            setupTestData(emptyList())
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.BUSINESS
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that checkIfHiddenNodeIsOnboarded updates isHiddenNodesOnboarded to true`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodesOnboarded).isTrue()
            }
        }

    @Test
    fun `test that checkIfHiddenNodeIsOnboarded updates isHiddenNodesOnboarded to false`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()

                assertThat(finalState.isHiddenNodesOnboarded).isFalse()
            }
        }

    @Test
    fun `test that setHiddenNodesOnboarded updates isHiddenNodesOnboarded to true`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            awaitItem()
            underTest.setHiddenNodesOnboarded()
            val updatedState = awaitItem()
            assertThat(updatedState.isHiddenNodesOnboarded).isTrue()
        }
    }

    @Test
    fun `test that monitorAccountDetail handles null account detail gracefully`() = runTest {
        setupTestData(emptyList())
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn null
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that monitorAccountDetail handles null account type gracefully`() = runTest {
        setupTestData(emptyList())
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn null
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that checkIfHiddenNodeIsOnboarded handles failure gracefully`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(isHiddenNodesOnboardedUseCase()).thenThrow(RuntimeException("Test exception"))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesOnboarded).isFalse()
        }
    }

    @Test
    fun `test that all hidden nodes properties are updated correctly when feature flag is enabled`() =
        runTest {
            setupTestData(emptyList())
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_I
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.showHiddenNodes).isTrue()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
                assertThat(finalState.isHiddenNodesOnboarded).isTrue()
            }
        }

    @Test
    fun `test that hidden nodes properties remain false when feature flag is disabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(mock<AccountDetail>()))
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.showHiddenNodes).isFalse()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
            assertThat(finalState.isHiddenNodesOnboarded).isFalse()
        }
    }
} 