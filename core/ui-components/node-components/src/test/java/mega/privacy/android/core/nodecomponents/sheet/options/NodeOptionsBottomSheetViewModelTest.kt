package mega.privacy.android.core.nodecomponents.sheet.options

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.util.isOutShare
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.DefaultGetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper = mock()
    private val getContactItemFromInShareFolder: DefaultGetContactItemFromInShareFolder = mock()
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase = mock()
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()
    private val nodeBottomSheetActionMapper = mock<NodeBottomSheetActionMapper>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val sampleFileNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
        on { name } doReturn "test_file.txt"
        on { isIncomingShare } doReturn false
    }

    private val sampleFolderNode = mock<TypedFolderNode>().stub {
        on { id } doReturn NodeId(456)
        on { name } doReturn "test_folder"
        on { isIncomingShare } doReturn true
    }

    private val sampleOutShareNode = mock<TypedFolderNode>().stub {
        on { id } doReturn NodeId(789)
        on { name } doReturn "shared_file.txt"
        on { isIncomingShare } doReturn false
        on { isShared } doReturn true
    }

    @BeforeEach
    fun initViewModel() {
        // Set up the MonitorConnectivityUseCase mock
        doReturn(flowOf(true)).whenever(monitorConnectivityUseCase)()

        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = nodeBottomSheetActionMapper,
            cloudDriveBottomSheetOptions = { emptySet() },
            rubbishBinBottomSheetOptions = { emptySet() },
            incomingSharesBottomSheetOptions = { emptySet() },
            outgoingSharesBottomSheetOptions = { emptySet() },
            linksBottomSheetOptions = { emptySet() },
            backupsBottomSheetOptions = { emptySet() },
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            nodeAccessPermissionIconMapper = nodeAccessPermissionIconMapper,
            getContactItemFromInShareFolder = getContactItemFromInShareFolder,
            getOutShareByNodeIdUseCase = getOutShareByNodeIdUseCase,
            getContactFromEmailUseCase = getContactFromEmailUseCase,
        )
    }

    @Test
    fun `test that get bottom sheet option invokes getNodeByIdUseCase`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            emptyList()
        )

        viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

        verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
        verify(isNodeInRubbishBinUseCase).invoke(sampleFileNode.id)
        verify(isNodeInBackupsUseCase).invoke(sampleFileNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleFileNode.id)
    }

    @Test
    fun `test that getBottomSheetOptions updates state with node information when successful`() =
        runTest {
            val mockActions = listOf(
                NodeActionModeMenuItem(1, 1, mock()),
                NodeActionModeMenuItem(1, 2, mock())
            )

            whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(mockActions)
            whenever(nodeAccessPermissionIconMapper(any())).thenReturn(123)

            viewModel.uiState.test {
                // Initial state
                val initialState = awaitItem()
                assertThat(initialState.name).isEmpty()
                assertThat(initialState.node).isNull()
                assertThat(initialState.actions).isEmpty()

                // Trigger the action
                viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

                // Wait for state update
                val updatedState = awaitItem()
                assertThat(updatedState.name).isEqualTo(sampleFileNode.name)
                assertThat(updatedState.node).isEqualTo(sampleFileNode)
                assertThat(updatedState.actions).isNotEmpty()
            }
        }

    @Test
    fun `test that getBottomSheetOptions handles exceptions gracefully`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenThrow(RuntimeException("Network error"))

        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.node).isNull()
            assertThat(initialState.actions).isEmpty()

            // Trigger the action
            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            // Wait for state update
            val updatedState = awaitItem()
            assertThat(updatedState.node).isNull()
            assertThat(updatedState.actions).isEmpty()
        }
    }

    @Test
    fun `test that getShareInfo retrieves contact info for incoming share folder`() = runTest {
        val contactItem = mock<ContactItem>().stub {
            on { contactData } doReturn ContactData("John Doe", null, null, mock())
        }

        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFolderNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.READ)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )
        whenever(getContactItemFromInShareFolder(any(), any())).thenReturn(contactItem)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(
                sampleFolderNode.id.longValue,
                NodeSourceType.INCOMING_SHARES
            )

            // Wait for initial state update
            val stateWithNode = awaitItem()
            assertThat(stateWithNode.node).isEqualTo(sampleFolderNode)

            // Wait for share info update
            val stateWithShareInfo = awaitItem()
            assertThat(stateWithShareInfo.shareInfo).isEqualTo("John Doe")
        }
    }

    @Test
    fun `test that getShareInfo handles incoming share folder error gracefully`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFolderNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.READ)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )
        whenever(getContactItemFromInShareFolder(any(), any())).thenThrow(RuntimeException("Contact error"))

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(
                sampleFolderNode.id.longValue,
                NodeSourceType.INCOMING_SHARES
            )

            // Should not crash and should still have valid state
            val state = awaitItem()
            assertThat(state.node).isNotNull()
        }
    }

    @Test
    fun `test that getShareInfo retrieves out share info for single share`() = runTest {
        val outShare = mock<ShareData>().stub {
            on { user } doReturn "user@example.com"
        }
        val contactItem = mock<ContactItem>().stub {
            on { contactData } doReturn ContactData("Jane Doe", null, null, mock())
        }

        whenever(getNodeByIdUseCase(any())).thenReturn(sampleOutShareNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )
        whenever(getOutShareByNodeIdUseCase(any())).thenReturn(listOf(outShare))
        whenever(getContactFromEmailUseCase(any(), any())).thenReturn(contactItem)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(
                sampleOutShareNode.id.longValue,
                NodeSourceType.OUTGOING_SHARES
            )

            // Wait for initial state update
            val stateWithNode = awaitItem()
            assertThat(stateWithNode.node).isEqualTo(sampleOutShareNode)

            // Wait for share info update
            val stateWithShareInfo = awaitItem()
            assertThat(stateWithShareInfo.shareInfo).isEqualTo("Jane Doe")
        }
    }

    @Test
    fun `test that getShareInfo sets outgoing shares for multiple shares`() = runTest {
        val outShares = listOf(
            mock<ShareData>().stub { on { user } doReturn "user1@example.com" },
            mock<ShareData>().stub { on { user } doReturn "user2@example.com" }
        )

        whenever(getNodeByIdUseCase(any())).thenReturn(sampleOutShareNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )
        whenever(getOutShareByNodeIdUseCase(any())).thenReturn(outShares)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(
                sampleOutShareNode.id.longValue,
                NodeSourceType.OUTGOING_SHARES
            )

            // Wait for initial state update
            val stateWithNode = awaitItem()
            assertThat(stateWithNode.node).isEqualTo(sampleOutShareNode)

            // Wait for outgoing shares update
            val stateWithOutgoingShares = awaitItem()
            assertThat(stateWithOutgoingShares.outgoingShares).isEqualTo(outShares)
        }
    }

    @Test
    fun `test that getAccessPermissionIcon returns icon only for incoming shares`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFolderNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.READ)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )
        whenever(nodeAccessPermissionIconMapper(AccessPermission.READ)).thenReturn(456)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(
                sampleFolderNode.id.longValue,
                NodeSourceType.INCOMING_SHARES
            )

            // Wait for state update
            val state = awaitItem()
            assertThat(state.accessPermissionIcon).isEqualTo(456)
        }
    }

    @Test
    fun `test that getAccessPermissionIcon returns null for non-incoming shares`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger the action
            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            // Wait for state update
            val state = awaitItem()
            assertThat(state.accessPermissionIcon).isNull()
        }
    }

    @Test
    fun `test that onConsumeErrorState consumes error`() = runTest {
        // First trigger an error by setting up a scenario that would cause an error
        whenever(getNodeByIdUseCase(any())).thenReturn(null)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger error
            viewModel.getBottomSheetOptions(999L, NodeSourceType.CLOUD_DRIVE)
            awaitItem() // Wait for error state

            // Consume error
            viewModel.onConsumeErrorState()

            // Verify error state is updated
            val finalState = awaitItem()
            assertThat(finalState).isNotNull()
        }
    }

    @Test
    fun `test that getBottomSheetOptions resets state before processing`() = runTest {
        // First set some state
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // First call
            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)
            
            // Verify state is set
            val firstState = awaitItem()
            assertThat(firstState.node).isNotNull()
            assertThat(firstState.actions).isNotEmpty()

            // Now call with a different node
            val differentNode = mock<TypedFileNode>().stub {
                on { id } doReturn NodeId(999)
                on { name } doReturn "different_file.txt"
                on { isIncomingShare } doReturn false
            }
            whenever(getNodeByIdUseCase(NodeId(999))).thenReturn(differentNode)

            viewModel.getBottomSheetOptions(999L, NodeSourceType.CLOUD_DRIVE)

            // First we get the reset state (node = null, actions = empty)
            val resetState = awaitItem()
            assertThat(resetState.node).isNull()
            assertThat(resetState.actions).isEmpty()

            // Then we get the updated state with the new node
            val secondState = awaitItem()
            assertThat(secondState.name).isEqualTo(differentNode.name)
            assertThat(secondState.node).isEqualTo(differentNode)
        }
    }
}
