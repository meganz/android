package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.FolderPreferenceKeys
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.GetContactUserNameFromDatabaseUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorOthersSortOrderUseCase
import mega.privacy.android.domain.usecase.shares.GetContactIncomingSharesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactSharedFoldersViewModelTest {

    private val getContactIncomingSharesUseCase: GetContactIncomingSharesUseCase = mock()
    private val getContactUserNameFromDatabaseUseCase: GetContactUserNameFromDatabaseUseCase =
        mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorFolderViewTypeUseCase: MonitorFolderViewTypeUseCase = mock()
    private val setFolderViewTypeUseCase: SetFolderViewTypeUseCase = mock()
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase = mock()
    private val setFolderSortOrderUseCase: SetFolderSortOrderUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val monitorOthersSortOrderUseCase: MonitorOthersSortOrderUseCase = mock()
    private val setOthersSortOrder: SetOthersSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()

    private val email = "john@mega.co.nz"

    private val folderNode = mock<ShareFolderNode> {
        on { id } doReturn NodeId(1L)
        on { name } doReturn "Shared folder"
    }

    private val fileNode = mock<ShareFileNode> {
        on { id } doReturn NodeId(2L)
        on { name } doReturn "Shared file"
    }

    private val sharedNodes: List<ShareNode> = listOf(folderNode, fileNode)

    @AfterEach
    fun tearDown() {
        reset(
            getContactIncomingSharesUseCase,
            getContactUserNameFromDatabaseUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorFolderViewTypeUseCase,
            setFolderViewTypeUseCase,
            monitorFolderSortOrderUseCase,
            setFolderSortOrderUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeUiItemMapper,
            monitorOthersSortOrderUseCase,
            setOthersSortOrder,
            nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase,
            snackbarEventQueue,
        )
    }

    @Test
    fun `test that contact name is set when it is resolved from the database`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().contactName).isEqualTo("John Doe")
        }
    }

    @Test
    fun `test that contact name is null when resolving it fails`() = runTest {
        stubUseCases()
        whenever(getContactUserNameFromDatabaseUseCase(email))
            .thenThrow(RuntimeException("Contact not found"))
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().contactName).isNull()
        }
    }

    @Test
    fun `test that shares are loaded for the contact email and the current sort order`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            val shares = awaitItem().shares
            assertThat(shares.isLoading).isFalse()
            assertThat(shares.items.map { it.node }).containsExactlyElementsIn(sharedNodes)
        }
        verify(getContactIncomingSharesUseCase)(
            email = email,
            sortOrder = SortOrder.ORDER_DEFAULT_ASC,
        )
    }

    @Test
    fun `test that items are empty when the contact has no shared folders`() = runTest {
        stubUseCases(nodes = emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val shares = awaitItem().shares
            assertThat(shares.isLoading).isFalse()
            assertThat(shares.items).isEmpty()
        }
    }

    @Test
    fun `test that loading stops when fetching the shares fails`() = runTest {
        stubUseCases()
        whenever(
            getContactIncomingSharesUseCase(
                email = email,
                sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            )
        ).thenThrow(RuntimeException("Failed to fetch shares"))
        val underTest = createViewModel()

        underTest.uiState.test {
            val shares = awaitItem().shares
            assertThat(shares.isLoading).isFalse()
            assertThat(shares.items).isEmpty()
        }
    }

    @Test
    fun `test that an error message is queued when fetching the shares fails`() = runTest {
        stubUseCases()
        whenever(
            getContactIncomingSharesUseCase(
                email = email,
                sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            )
        ).thenThrow(RuntimeException("Failed to fetch shares"))
        createViewModel()

        verify(snackbarEventQueue).queueMessage(sharedR.string.general_request_failed_message)
    }

    @Test
    fun `test that no error message is queued when the shares are fetched`() = runTest {
        stubUseCases()
        createViewModel()

        verifyNoInteractions(snackbarEventQueue)
    }

    @Test
    fun `test that the contact verification warning is passed to the node ui item mapper`() =
        runTest {
            stubUseCases(isContactVerificationOn = true)
            createViewModel()

            verify(nodeUiItemMapper)(
                nodeList = sharedNodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true,
            )
        }

    @Test
    fun `test that contact verification is off when checking the warning fails`() = runTest {
        stubUseCases()
        whenever(getContactVerificationWarningUseCase())
            .thenThrow(RuntimeException("Failed to read the setting"))
        createViewModel()

        verify(nodeUiItemMapper)(
            nodeList = sharedNodes,
            existingItems = emptyList(),
            nodeSourceType = NodeSourceType.INCOMING_SHARES,
            isContactVerificationOn = false,
        )
    }

    @Test
    fun `test that the view type is updated when the monitored view type changes`() = runTest {
        stubUseCases(viewType = ViewType.GRID)
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().shares.currentViewType).isEqualTo(ViewType.GRID)
        }
    }

    @Test
    fun `test that the sort order and its configuration are updated when the monitored sort order changes`() =
        runTest {
            val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            val sortConfiguration = NodeSortConfiguration(
                sortOption = NodeSortOption.Modified,
                sortDirection = SortDirection.Descending,
            )
            stubUseCases(sortOrder = sortOrder, sortConfiguration = sortConfiguration)
            val underTest = createViewModel()

            underTest.uiState.test {
                val shares = awaitItem().shares
                assertThat(shares.selectedSortOrder).isEqualTo(sortOrder)
                assertThat(shares.selectedSortConfiguration).isEqualTo(sortConfiguration)
            }
        }

    @Test
    fun `test that navigateBack is triggered when the shared folder is removed`() = runTest {
        stubUseCases(nodeUpdates = flowOf(NodeChanges.Remove))
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().shares.navigateBack).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that shares are reloaded when a node update other than remove is received`() =
        runTest {
            stubUseCases(nodeUpdates = flowOf(NodeChanges.Name))
            createViewModel()

            verify(getContactIncomingSharesUseCase, times(2))(
                email = email,
                sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            )
        }

    @Test
    fun `test that NavigateBackEventConsumed consumes the navigate back event`() = runTest {
        stubUseCases(nodeUpdates = flowOf(NodeChanges.Remove))
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().shares.navigateBack).isEqualTo(triggered)

            underTest.processAction(IncomingSharesAction.NavigateBackEventConsumed)

            assertThat(awaitItem().shares.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that ItemClicked triggers navigateToFolderEvent when the item is a folder`() =
        runTest {
            stubUseCases()
            val underTest = createViewModel()

            underTest.uiState.test {
                val item = awaitItem().shares.items.first()

                underTest.processAction(IncomingSharesAction.ItemClicked(item))

                val event = awaitItem().shares.navigateToFolderEvent
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((event as StateEventWithContentTriggered).content).isEqualTo(folderNode)
            }
        }

    @Test
    fun `test that ItemClicked does not trigger navigateToFolderEvent when the item is a file`() =
        runTest {
            stubUseCases()
            val underTest = createViewModel()

            underTest.uiState.test {
                val item = awaitItem().shares.items.last()

                underTest.processAction(IncomingSharesAction.ItemClicked(item))

                expectNoEvents()
            }
        }

    @Test
    fun `test that NavigateToFolderEventConsumed consumes the navigate to folder event`() =
        runTest {
            stubUseCases()
            val underTest = createViewModel()

            underTest.uiState.test {
                val item = awaitItem().shares.items.first()
                underTest.processAction(IncomingSharesAction.ItemClicked(item))
                awaitItem()

                underTest.processAction(IncomingSharesAction.NavigateToFolderEventConsumed)

                assertThat(awaitItem().shares.navigateToFolderEvent).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that ItemLongClicked selects the item`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            val item = awaitItem().shares.items.first()

            underTest.processAction(IncomingSharesAction.ItemLongClicked(item))

            val shares = awaitItem().shares
            assertThat(shares.isInSelectionMode).isTrue()
            assertThat(shares.selectedNodes).containsExactly(folderNode)
        }
    }

    @Test
    fun `test that ItemLongClicked deselects an already selected item`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            val item = awaitItem().shares.items.first()
            underTest.processAction(IncomingSharesAction.ItemLongClicked(item))
            val selectedItem = awaitItem().shares.items.first()

            underTest.processAction(IncomingSharesAction.ItemLongClicked(selectedItem))

            assertThat(awaitItem().shares.isInSelectionMode).isFalse()
        }
    }

    @Test
    fun `test that ItemClicked toggles the selection when in selection mode`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            val items = awaitItem().shares.items
            underTest.processAction(IncomingSharesAction.ItemLongClicked(items.first()))
            awaitItem()

            underTest.processAction(IncomingSharesAction.ItemClicked(items.last()))

            val shares = awaitItem().shares
            assertThat(shares.selectedItemsCount).isEqualTo(2)
            assertThat(shares.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that SelectAllItems selects every item`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()

            underTest.processAction(IncomingSharesAction.SelectAllItems)

            val shares = awaitItem().shares
            assertThat(shares.isAllSelected).isTrue()
            assertThat(shares.selectedItemsCount).isEqualTo(sharedNodes.size)
        }
    }

    @Test
    fun `test that DeselectAllItems clears the selection`() = runTest {
        stubUseCases()
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            underTest.processAction(IncomingSharesAction.SelectAllItems)
            awaitItem()

            underTest.processAction(IncomingSharesAction.DeselectAllItems)

            assertThat(awaitItem().shares.isInSelectionMode).isFalse()
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked switches the view type from list to grid`() = runTest {
        stubUseCases(viewType = ViewType.LIST)
        val underTest = createViewModel()

        underTest.processAction(IncomingSharesAction.ChangeViewTypeClicked)

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            viewType = eq(ViewType.GRID),
            currentSortOrder = eq(SortOrder.ORDER_DEFAULT_ASC),
            orElse = any(),
        )
    }

    @Test
    fun `test that ChangeViewTypeClicked switches the view type from grid to list`() = runTest {
        stubUseCases(viewType = ViewType.GRID)
        val underTest = createViewModel()

        underTest.processAction(IncomingSharesAction.ChangeViewTypeClicked)

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            viewType = eq(ViewType.LIST),
            currentSortOrder = eq(SortOrder.ORDER_DEFAULT_ASC),
            orElse = any(),
        )
    }

    @Test
    fun `test that setSortOrder sets the folder sort order for incoming shares`() = runTest {
        stubUseCases()
        val sortConfiguration = NodeSortConfiguration(
            sortOption = NodeSortOption.Modified,
            sortDirection = SortDirection.Descending,
        )
        whenever(nodeSortConfigurationUiMapper(sortConfiguration))
            .thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        val underTest = createViewModel()

        underTest.setSortOrder(sortConfiguration)

        verify(setFolderSortOrderUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            sortOrder = eq(SortOrder.ORDER_MODIFICATION_DESC),
            currentViewType = eq(ViewType.LIST),
            orElse = any(),
        )
    }

    @Test
    fun `test that the folder sort order is not set when mapping the configuration fails`() =
        runTest {
            stubUseCases()
            val sortConfiguration = NodeSortConfiguration(
                sortOption = NodeSortOption.Modified,
                sortDirection = SortDirection.Descending,
            )
            whenever(nodeSortConfigurationUiMapper(sortConfiguration))
                .thenThrow(RuntimeException("Unsupported sort option"))
            val underTest = createViewModel()

            underTest.setSortOrder(sortConfiguration)

            verifyNoInteractions(setFolderSortOrderUseCase)
        }

    private suspend fun stubUseCases(
        nodes: List<ShareNode> = sharedNodes,
        viewType: ViewType = ViewType.LIST,
        sortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
        sortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        nodeUpdates: Flow<NodeChanges> = emptyFlow(),
        isContactVerificationOn: Boolean = false,
    ) {
        val nodeUiItems = nodes.map { NodeUiItem<TypedNode>(node = it, isSelected = false) }
        whenever(getContactUserNameFromDatabaseUseCase(email)) doReturn "John Doe"
        whenever(monitorViewTypeUseCase()) doReturn flowOf(viewType)
        whenever(monitorFolderViewTypeUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
        whenever(monitorOthersSortOrderUseCase()) doReturn flowOf(sortOrder)
        whenever(monitorFolderSortOrderUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
        whenever(nodeSortConfigurationUiMapper(sortOrder)) doReturn sortConfiguration
        whenever(
            monitorNodeUpdatesByIdUseCase(NodeId(-1L), NodeSourceType.INCOMING_SHARES)
        ) doReturn nodeUpdates
        whenever(getContactVerificationWarningUseCase()) doReturn isContactVerificationOn
        whenever(
            getContactIncomingSharesUseCase(email = email, sortOrder = sortOrder)
        ) doReturn nodes
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = isContactVerificationOn,
            )
        ) doReturn nodeUiItems
    }

    private fun createViewModel() = ContactSharedFoldersViewModel(
        getContactIncomingSharesUseCase = getContactIncomingSharesUseCase,
        getContactUserNameFromDatabaseUseCase = getContactUserNameFromDatabaseUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorFolderViewTypeUseCase = monitorFolderViewTypeUseCase,
        setFolderViewTypeUseCase = setFolderViewTypeUseCase,
        monitorFolderSortOrderUseCase = monitorFolderSortOrderUseCase,
        setFolderSortOrderUseCase = setFolderSortOrderUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        monitorOthersSortOrderUseCase = monitorOthersSortOrderUseCase,
        setOthersSortOrder = setOthersSortOrder,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        snackbarEventQueue = snackbarEventQueue,
        args = ContactSharedFoldersViewModel.Args(email = email),
    )
}
