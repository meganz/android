package mega.privacy.android.feature.clouddrive.presentation.folderbrowser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodePage
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.PageRequest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.GetChildrenCountUseCase
import mega.privacy.android.domain.usecase.node.HandleToBase64UseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.clouddrive.GetNodeChildrenPageUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.FolderBrowserViewModel.Companion.LOADED_COUNT_KEY
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.FolderBrowserViewModel.Companion.PAGE_SIZE
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model.FolderBrowserAction
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * The pager publishes re-reads only while something collects [FolderBrowserViewModel.uiState],
 * so every test about a folder change collects the state (Turbine or [observe]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderBrowserViewModelTest {

    @JvmField
    @RegisterExtension
    val analyticsExtension = AnalyticsTestExtension()

    private val getNodeInfoByIdUseCase = mock<GetNodeInfoByIdUseCase>()
    private val getNodeChildrenPageUseCase = mock<GetNodeChildrenPageUseCase>()
    private val getChildrenCountUseCase = mock<GetChildrenCountUseCase>()
    private val setViewTypeUseCase = mock<SetViewType>()
    private val monitorViewTypeUseCase = mock<MonitorViewType>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val nodeViewItemMapper = mock<NodeViewItemMapper>()
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>()
    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase>()
    private val getIncomingShareParentUserEmailUseCase =
        mock<GetIncomingShareParentUserEmailUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val handleToBase64UseCase = mock<HandleToBase64UseCase>()
    private val monitorFolderViewTypeUseCase = mock<MonitorFolderViewTypeUseCase>()
    private val monitorFolderSortOrderUseCase = mock<MonitorFolderSortOrderUseCase>()
    private val setFolderViewTypeUseCase = mock<SetFolderViewTypeUseCase>()
    private val setFolderSortOrderUseCase = mock<SetFolderSortOrderUseCase>()

    private val folderId = NodeId(100L)
    private val rootId = NodeId(7L)
    private val nodeUpdates = MutableSharedFlow<NodeChanges>(extraBufferCapacity = 8)
    private val sortOrders = MutableStateFlow(SortOrder.ORDER_DEFAULT_ASC)
    private val viewTypes = MutableStateFlow(ViewType.LIST)
    private val hiddenNodesEnabled = MutableStateFlow(false)
    private val showHiddenItems = MutableStateFlow(false)
    private val pageRequests = mutableListOf<PageRequest>()
    private var source: List<TypedNode> = emptyList()
    private var highlightedId: NodeId? = null
    private val createdViewModels = mutableListOf<FolderBrowserViewModel>()

    @BeforeEach
    fun setUp() {
        reset(
            getNodeInfoByIdUseCase, getNodeChildrenPageUseCase,
            getChildrenCountUseCase, setViewTypeUseCase, monitorViewTypeUseCase,
            monitorShowHiddenItemsUseCase, monitorNodeUpdatesByIdUseCase,
            monitorHiddenNodesEnabledUseCase, nodeViewItemMapper, getRootNodeIdUseCase,
            setCloudSortOrderUseCase, nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase, areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase, getNodeAccessPermission, isNodeInBackupsUseCase,
            monitorSortCloudOrderUseCase, handleToBase64UseCase,
            monitorFolderViewTypeUseCase, monitorFolderSortOrderUseCase, setFolderViewTypeUseCase,
            setFolderSortOrderUseCase,
        )
        pageRequests.clear()
        source = nodes(1L..PAGE_SIZE + 10L)
        highlightedId = null
        sortOrders.value = SortOrder.ORDER_DEFAULT_ASC
        viewTypes.value = ViewType.LIST
        hiddenNodesEnabled.value = false
        showHiddenItems.value = false
        stubDefaults()
    }

    /**
     * A ViewModel's collectors outlive its test on the unconfined Main dispatcher and would keep
     * reacting to the shared stub flows, so every ViewModel is cancelled when its test ends
     */
    @AfterEach
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
    }

    private fun stubDefaults() {
        wheneverBlocking { getRootNodeIdUseCase() }.thenReturn(rootId)
        val folderInfo = nodeInfo("Test folder", decrypted = true)
        wheneverBlocking { getNodeInfoByIdUseCase(any()) }.thenReturn(folderInfo)
        wheneverBlocking { getNodeAccessPermission(any()) }.thenReturn(AccessPermission.OWNER)
        wheneverBlocking { isNodeInBackupsUseCase(any()) }.thenReturn(false)
        whenever(monitorNodeUpdatesByIdUseCase(any(), any(), any())).thenReturn(nodeUpdates)
        whenever(monitorFolderSortOrderUseCase(folderKey = any(), orElse = any())).thenReturn(
            sortOrders
        )
        whenever(monitorSortCloudOrderUseCase()).thenReturn(sortOrders)
        whenever(nodeSortConfigurationUiMapper(any<SortOrder>(), any())).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>())).thenReturn(SortOrder.ORDER_SIZE_DESC)
        whenever(monitorFolderViewTypeUseCase(folderKey = any(), orElse = any())).thenReturn(
            viewTypes
        )
        whenever(monitorViewTypeUseCase()).thenReturn(viewTypes)
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(hiddenNodesEnabled)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(showHiddenItems)
        wheneverBlocking { getContactVerificationWarningUseCase() }.thenReturn(false)
        wheneverBlocking { areCredentialsVerifiedUseCase(any()) }.thenReturn(true)
        wheneverBlocking { getIncomingShareParentUserEmailUseCase(any()) }.thenReturn(null)
        wheneverBlocking { getChildrenCountUseCase(any()) }.thenAnswer { source.size }
        wheneverBlocking { handleToBase64UseCase(any()) }.thenReturn("folderKey")
        wheneverBlocking {
            nodeViewItemMapper(
                nodeList = any(),
                nodeSourceType = any(),
                highlightedNodeId = anyOrNull(),
                isHiddenNodesEnabled = any(),
                highlightedNames = anyOrNull(),
                isContactVerificationOn = any(),
                isPublicNodes = any(),
            )
        }.thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val nodes = invocation.arguments[0] as List<TypedNode>
            nodes.map { node ->
                previewFileNodeUiItem(id = node.id.longValue).copy(
                    node = node,
                    isHighlighted = node.id == highlightedId,
                )
            }
        }
        wheneverBlocking {
            getNodeChildrenPageUseCase(
                any(),
                any(),
                any(),
                any()
            )
        }.thenAnswer { invocation ->
            val request = invocation.arguments[3] as PageRequest
            pageRequests += request
            val rows = source.drop(request.offset).take(request.limit)
            NodePage(
                nodes = rows,
                nextOffset = request.offset + rows.size,
                hasMore = source.size > request.offset + request.limit,
            )
        }
    }

    private fun nodes(ids: LongRange): List<TypedNode> =
        ids.map { previewFileNodeUiItem(id = it).node }

    private fun nodeInfo(name: String, decrypted: Boolean) = mock<NodeInfo> {
        on { this.name } doReturn name
        on { isNodeKeyDecrypted } doReturn decrypted
    }

    private fun createViewModel(
        currentFolderId: NodeId = folderId,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        highlightedNodeId: NodeId? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = FolderBrowserViewModel(
        getNodeInfoByIdUseCase = getNodeInfoByIdUseCase,
        getNodeChildrenPageUseCase = getNodeChildrenPageUseCase,
        getChildrenCountUseCase = getChildrenCountUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
        nodeViewItemMapper = nodeViewItemMapper,
        getRootNodeIdUseCase = getRootNodeIdUseCase,
        setCloudSortOrderUseCase = setCloudSortOrderUseCase,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
        getIncomingShareParentUserEmailUseCase = getIncomingShareParentUserEmailUseCase,
        getNodeAccessPermission = getNodeAccessPermission,
        isNodeInBackupsUseCase = isNodeInBackupsUseCase,
        monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        handleToBase64UseCase = handleToBase64UseCase,
        monitorFolderViewTypeUseCase = monitorFolderViewTypeUseCase,
        monitorFolderSortOrderUseCase = monitorFolderSortOrderUseCase,
        setFolderViewTypeUseCase = setFolderViewTypeUseCase,
        setFolderSortOrderUseCase = setFolderSortOrderUseCase,
        savedStateHandle = savedStateHandle,
        args = FolderBrowserViewModel.Args(
            currentFolderId = currentFolderId,
            title = LocalizedText.Literal("Initial"),
            nodeSourceType = nodeSourceType,
            highlightedNodeId = highlightedNodeId,
            highlightedNodeNames = null,
        ),
    ).also { createdViewModels += it }

    /** Keeps [FolderBrowserViewModel.uiState] collected so the pager's publish gate is open */
    private fun TestScope.observe(underTest: FolderBrowserViewModel) {
        backgroundScope.launch { underTest.uiState.collect {} }
        runCurrent()
    }

    private fun FolderBrowserViewModel.itemIds() = uiState.value.items.map { it.id.longValue }

    @Test
    fun `test that initial state shows the title from the arguments while loading`() = runTest {
        wheneverBlocking { getRootNodeIdUseCase() }.doSuspendableAnswer { CompletableDeferred<NodeId>().await() }
        val underTest = createViewModel(currentFolderId = NodeId(-1L))

        val state = underTest.uiState.value
        assertThat(state.title).isEqualTo(LocalizedText.Literal("Initial"))
        assertThat(state.isLoading).isTrue()
        assertThat(state.isCloudDriveRoot).isTrue()
        assertThat(state.currentFolderId).isNull()
    }

    @Test
    fun `test that the first page is requested at offset 0 with PAGE_SIZE rows and shown as fully loaded`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(pageRequests).containsExactly(PageRequest(offset = 0, limit = PAGE_SIZE))
            assertThat(underTest.itemIds()).isEqualTo((1L..PAGE_SIZE).toList())
            assertThat(underTest.uiState.value.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(underTest.uiState.value.hasMore).isTrue()
            assertThat(underTest.uiState.value.currentFolderId).isEqualTo(folderId)
        }

    @Test
    fun `test that the root folder is resolved with getRootNodeIdUseCase and paged under its id`() =
        runTest {
            val underTest = createViewModel(currentFolderId = NodeId(-1L))
            advanceUntilIdle()

            assertThat(underTest.uiState.value.currentFolderId).isEqualTo(rootId)
            assertThat(underTest.uiState.value.isCloudDriveRoot).isTrue()
            verify(getNodeChildrenPageUseCase).invoke(eq(rootId), any(), any(), any())
        }

    @Test
    fun `test that the root falls back to NodeId(-1) when getRootNodeIdUseCase returns null`() =
        runTest {
            wheneverBlocking { getRootNodeIdUseCase() }.thenReturn(null)
            val underTest = createViewModel(currentFolderId = NodeId(-1L))
            advanceUntilIdle()

            assertThat(underTest.uiState.value.currentFolderId).isEqualTo(NodeId(-1L))
        }

    @Test
    fun `test that isCloudDriveRoot is false for a folder`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.isCloudDriveRoot).isFalse()
        verify(getRootNodeIdUseCase, never()).invoke()
    }

    @Test
    fun `test that the title is the folder name when the node key is decrypted`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.title).isEqualTo(LocalizedText.Literal("Test folder"))
    }

    @Test
    fun `test that the title is the undecrypted folder text when the node key is not decrypted`() =
        runTest {
            val undecrypted = nodeInfo("x", decrypted = false)
            wheneverBlocking { getNodeInfoByIdUseCase(any()) }.thenReturn(undecrypted)
            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.title)
                .isEqualTo(LocalizedText.StringRes(sharedR.string.shared_items_verify_credentials_undecrypted_folder))
        }

    @Test
    fun `test that write permission is granted for OWNER READWRITE and FULL`() = runTest {
        listOf(
            AccessPermission.OWNER,
            AccessPermission.READWRITE,
            AccessPermission.FULL
        ).forEach { permission ->
            wheneverBlocking { getNodeAccessPermission(any()) }.thenReturn(permission)
            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isTrue()
        }
    }

    @Test
    fun `test that write permission is denied for READ, no permission and a failing check`() =
        runTest {
            wheneverBlocking { getNodeAccessPermission(any()) }.thenReturn(AccessPermission.READ)
            assertThat(createViewModel().also { advanceUntilIdle() }.uiState.value.hasWritePermission).isFalse()

            wheneverBlocking { getNodeAccessPermission(any()) }.thenReturn(null)
            assertThat(createViewModel().also { advanceUntilIdle() }.uiState.value.hasWritePermission).isFalse()

            wheneverBlocking { getNodeAccessPermission(any()) }.thenThrow(IllegalStateException())
            assertThat(createViewModel().also { advanceUntilIdle() }.uiState.value.hasWritePermission).isFalse()
        }

    @Test
    fun `test that isNodeInBackups is read for the folder`() = runTest {
        wheneverBlocking { isNodeInBackupsUseCase(folderId.longValue) }.thenReturn(true)
        val underTest = createViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.isNodeInBackups).isTrue()
        assertThat(underTest.uiState.value.isUploadAllowed).isFalse()
    }

    @Test
    fun `test that a Remove update triggers navigateBack and the consumed action resets it`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()

            nodeUpdates.emit(NodeChanges.Remove)
            runCurrent()
            assertThat(underTest.uiState.value.navigateBack).isEqualTo(triggered)

            underTest.processAction(FolderBrowserAction.NavigateBackEventConsumed)
            assertThat(underTest.uiState.value.navigateBack).isEqualTo(consumed)
        }

    @Test
    fun `test that an Attributes update re-reads the loaded prefix in one call and replaces the items`() =
        runTest {
            val underTest = createViewModel()
            observe(underTest)
            advanceUntilIdle()
            source = source.filter { it.id.longValue != 3L }

            nodeUpdates.emit(NodeChanges.Attributes)
            advanceUntilIdle()

            assertThat(pageRequests.last()).isEqualTo(PageRequest(offset = 0, limit = PAGE_SIZE))
            assertThat(underTest.itemIds()).doesNotContain(3L)
            assertThat(underTest.itemIds()).hasSize(PAGE_SIZE)
            assertThat(underTest.uiState.value.reconcileVersion).isEqualTo(1)
            assertThat(underTest.uiState.value.navigateBack).isEqualTo(consumed)
        }

    @Test
    fun `test that an Attributes update refreshes the title and write permission`() = runTest {
        val underTest = createViewModel()
        observe(underTest)
        advanceUntilIdle()
        val renamed = nodeInfo("Renamed", decrypted = true)
        wheneverBlocking { getNodeInfoByIdUseCase(any()) }.thenReturn(renamed)

        nodeUpdates.emit(NodeChanges.Attributes)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.title).isEqualTo(LocalizedText.Literal("Renamed"))
        verify(getNodeAccessPermission, times(2)).invoke(folderId)
    }

    @Test
    fun `test that a folder change while the state is not collected is re-read once it is`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()
            val requestsBefore = pageRequests.size

            nodeUpdates.emit(NodeChanges.Attributes)
            advanceUntilIdle()
            assertThat(pageRequests).hasSize(requestsBefore)

            observe(underTest)
            advanceUntilIdle()
            assertThat(pageRequests).hasSize(requestsBefore + 1)
        }

    @Test
    fun `test that the view type follows the folder view type flow`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()
        assertThat(underTest.uiState.value.currentViewType).isEqualTo(ViewType.LIST)

        viewTypes.value = ViewType.GRID
        runCurrent()

        assertThat(underTest.uiState.value.currentViewType).isEqualTo(ViewType.GRID)
    }

    @Test
    fun `test that ChangeViewTypeClicked stores the view type for the folder and tracks the event`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.processAction(FolderBrowserAction.ChangeViewTypeClicked(ViewType.GRID))
            advanceUntilIdle()

            verify(setFolderViewTypeUseCase).invoke(
                folderKey = eq("folderKey"),
                viewType = eq(ViewType.GRID),
                currentSortOrder = eq(SortOrder.ORDER_DEFAULT_ASC),
                orElse = any(),
            )
            assertThat(analyticsExtension.events).contains(ViewModeGridMenuItemEvent)
        }

    @Test
    fun `test that the sort order and its configuration follow the folder sort order flow`() =
        runTest {
            val sizeConfiguration =
                NodeSortConfiguration.default.copy(sortOption = NodeSortOption.Size)
            whenever(
                nodeSortConfigurationUiMapper(
                    eq(SortOrder.ORDER_SIZE_DESC),
                    any()
                )
            ).thenReturn(sizeConfiguration)
            val underTest = createViewModel()
            advanceUntilIdle()

            sortOrders.value = SortOrder.ORDER_SIZE_DESC
            advanceUntilIdle()

            assertThat(underTest.uiState.value.selectedSortOrder).isEqualTo(SortOrder.ORDER_SIZE_DESC)
            assertThat(underTest.uiState.value.selectedSortConfiguration).isEqualTo(
                sizeConfiguration
            )
        }

    @Test
    fun `test that a sort order change starts paging again at offset 0 with the new order`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()

            sortOrders.value = SortOrder.ORDER_SIZE_DESC
            advanceUntilIdle()

            verify(getNodeChildrenPageUseCase).invoke(
                eq(folderId),
                eq(SortOrder.ORDER_SIZE_DESC),
                any(),
                eq(PageRequest(offset = 0, limit = PAGE_SIZE)),
            )
            assertThat(underTest.itemIds()).hasSize(PAGE_SIZE)
        }

    @Test
    fun `test that a sort order change keeps the items until the new first page lands`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()
        val hold = CompletableDeferred<Unit>()
        wheneverBlocking {
            getNodeChildrenPageUseCase(
                any(),
                eq(SortOrder.ORDER_SIZE_DESC),
                any(),
                any()
            )
        }
            .doSuspendableAnswer { invocation ->
                hold.await()
                val request = invocation.getArgument<PageRequest>(3)
                NodePage(
                    nodes = source.take(request.limit).reversed(),
                    nextOffset = request.limit,
                    hasMore = false
                )
            }

        sortOrders.value = SortOrder.ORDER_SIZE_DESC
        runCurrent()

        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.itemIds()).isEqualTo((1L..PAGE_SIZE).toList())

        hold.complete(Unit)
        advanceUntilIdle()

        assertThat(underTest.itemIds().first()).isEqualTo(PAGE_SIZE.toLong())
    }

    @Test
    fun `test that setCloudSortOrder stores the mapped order for the folder`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setCloudSortOrder(NodeSortConfiguration.default.copy(sortOption = NodeSortOption.Size))
        advanceUntilIdle()

        verify(setFolderSortOrderUseCase).invoke(
            folderKey = eq("folderKey"),
            sortOrder = eq(SortOrder.ORDER_SIZE_DESC),
            currentViewType = eq(ViewType.LIST),
            orElse = any(),
        )
    }

    @Test
    fun `test that hidden nodes are excluded from the SDK query when enabled and not shown`() =
        runTest {
            hiddenNodesEnabled.value = true
            showHiddenItems.value = false
            val underTest = createViewModel()
            advanceUntilIdle()

            verify(getNodeChildrenPageUseCase).invoke(eq(folderId), any(), eq(true), any())
            assertThat(underTest.uiState.value.total).isNull()
            verify(getChildrenCountUseCase, never()).invoke(any())
        }

    @Test
    fun `test that hidden nodes are included and the total is read when the feature is off`() =
        runTest {
            val underTest = createViewModel()
            advanceUntilIdle()

            verify(getNodeChildrenPageUseCase).invoke(eq(folderId), any(), eq(false), any())
            assertThat(underTest.uiState.value.total).isEqualTo(source.size)
        }

    @Test
    fun `test that hidden nodes are never excluded in a shared folder`() = runTest {
        hiddenNodesEnabled.value = true
        showHiddenItems.value = false
        createViewModel(nodeSourceType = NodeSourceType.INCOMING_SHARES)
        advanceUntilIdle()

        verify(getNodeChildrenPageUseCase).invoke(eq(folderId), any(), eq(false), any())
    }

    @Test
    fun `test that the contact banner is shown for an incoming share from an unverified contact`() =
        runTest {
            wheneverBlocking { getContactVerificationWarningUseCase() }.thenReturn(true)
            wheneverBlocking { getIncomingShareParentUserEmailUseCase(folderId) }.thenReturn("owner@mega.nz")
            wheneverBlocking { areCredentialsVerifiedUseCase("owner@mega.nz") }.thenReturn(false)
            val underTest = createViewModel(nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.showContactNotVerifiedBanner).isTrue()
        }

    @Test
    fun `test that the contact banner is hidden for an incoming share from a verified contact`() =
        runTest {
            wheneverBlocking { getContactVerificationWarningUseCase() }.thenReturn(true)
            wheneverBlocking { getIncomingShareParentUserEmailUseCase(folderId) }.thenReturn("owner@mega.nz")
            wheneverBlocking { areCredentialsVerifiedUseCase("owner@mega.nz") }.thenReturn(true)
            val underTest = createViewModel(nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.showContactNotVerifiedBanner).isFalse()
        }

    @Test
    fun `test that contact verification is never checked for Cloud Drive`() = runTest {
        wheneverBlocking { getContactVerificationWarningUseCase() }.thenReturn(true)
        val underTest = createViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.showContactNotVerifiedBanner).isFalse()
        verify(getContactVerificationWarningUseCase, never()).invoke()
        verify(getIncomingShareParentUserEmailUseCase, never()).invoke(any())
    }

    @Test
    fun `test that contact verification is passed to the mapper for a shared folder`() = runTest {
        wheneverBlocking { getContactVerificationWarningUseCase() }.thenReturn(true)
        createViewModel(nodeSourceType = NodeSourceType.OUTGOING_SHARES)
        advanceUntilIdle()

        verify(nodeViewItemMapper).invoke(
            nodeList = any(),
            nodeSourceType = eq(NodeSourceType.OUTGOING_SHARES),
            highlightedNodeId = anyOrNull(),
            isHiddenNodesEnabled = any(),
            highlightedNames = anyOrNull(),
            isContactVerificationOn = eq(true),
            isPublicNodes = any(),
        )
    }

    @Test
    fun `test that a visible range near the end loads the next page`() = runTest {
        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.processAction(FolderBrowserAction.VisibleRangeChanged(PAGE_SIZE - 5..PAGE_SIZE - 1))
        advanceUntilIdle()

        assertThat(pageRequests.last().offset).isEqualTo(PAGE_SIZE)
        assertThat(underTest.itemIds()).hasSize(source.size)
        assertThat(underTest.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `test that pages keep loading until the highlighted node is in the list`() = runTest {
        source = nodes(1L..(PAGE_SIZE * 3L))
        highlightedId = NodeId(PAGE_SIZE * 2L + 1)
        val underTest = createViewModel(highlightedNodeId = highlightedId)
        advanceUntilIdle()

        assertThat(underTest.itemIds()).contains(PAGE_SIZE * 2L + 1)
        assertThat(underTest.uiState.value.items.any { it.isHighlighted }).isTrue()
        // The second request is widened to reach the node, so two requests cover it
        assertThat(pageRequests.map { it.offset }).containsExactly(0, PAGE_SIZE).inOrder()
    }

    @Test
    fun `test that highlight paging stops when the folder ends without the node`() = runTest {
        source = nodes(1L..(PAGE_SIZE * 2L))
        val underTest = createViewModel(highlightedNodeId = NodeId(999_999L))
        advanceUntilIdle()

        assertThat(underTest.itemIds()).hasSize(PAGE_SIZE * 2)
        assertThat(underTest.uiState.value.hasMore).isFalse()
    }

    @Test
    fun `test that a cancelled folder details refresh publishes nothing`() = runTest {
        val underTest = createViewModel()
        observe(underTest)
        advanceUntilIdle()
        val hold = CompletableDeferred<AccessPermission>()
        wheneverBlocking { getNodeAccessPermission(any()) }
            .doSuspendableAnswer { hold.await() }
            .thenReturn(AccessPermission.OWNER)

        nodeUpdates.emit(NodeChanges.Attributes)
        runCurrent()
        // A second change cancels the refresh that is still waiting on the permission
        nodeUpdates.emit(NodeChanges.Attributes)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.hasWritePermission).isTrue()
    }

    @Test
    fun `test that the title given for the root is kept when the root node cannot be read`() = runTest {
        wheneverBlocking { getNodeInfoByIdUseCase(any()) }.thenReturn(null)
        val underTest = createViewModel(currentFolderId = NodeId(-1L))
        advanceUntilIdle()

        assertThat(underTest.uiState.value.title).isEqualTo(LocalizedText.Literal("Initial"))
        verify(getNodeInfoByIdUseCase).invoke(rootId)
    }

    @Test
    fun `test that the loaded row count is saved as pages land`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val underTest = createViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()
        assertThat(savedStateHandle.get<Int>(LOADED_COUNT_KEY)).isEqualTo(PAGE_SIZE)

        underTest.processAction(FolderBrowserAction.VisibleRangeChanged(PAGE_SIZE - 5..PAGE_SIZE - 1))
        advanceUntilIdle()

        assertThat(savedStateHandle.get<Int>(LOADED_COUNT_KEY)).isEqualTo(source.size)
    }

    @Test
    fun `test that a saved row count is reloaded in one request after process death`() = runTest {
        source = nodes(1L..(PAGE_SIZE * 3L))
        val savedStateHandle = SavedStateHandle(mapOf(LOADED_COUNT_KEY to PAGE_SIZE + 120))
        val underTest = createViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        assertThat(pageRequests.first()).isEqualTo(PageRequest(offset = 0, limit = PAGE_SIZE + 120))
        assertThat(underTest.itemIds()).hasSize(PAGE_SIZE + 120)
    }

    @Test
    fun `test that a saved row count applies to the first pager only`() = runTest {
        source = nodes(1L..(PAGE_SIZE * 3L))
        val savedStateHandle = SavedStateHandle(mapOf(LOADED_COUNT_KEY to PAGE_SIZE * 2))
        createViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        sortOrders.value = SortOrder.ORDER_SIZE_DESC
        advanceUntilIdle()

        assertThat(pageRequests.last()).isEqualTo(PageRequest(offset = 0, limit = PAGE_SIZE))
    }

    @Test
    fun `test that uiState emits loading then the loaded page`() = runTest {
        val underTest = createViewModel()

        underTest.uiState.test {
            val loaded = expectMostRecentItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.items).hasSize(PAGE_SIZE)
        }
    }
}
