package mega.privacy.android.core.nodecomponents.components.selectionmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSelectionModeActionMapper
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionModeMenuItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class NodeSelectionModeViewModelTest {
    private lateinit var underTest: NodeSelectionModeViewModel

    private val cloudDriveOptions =
        mock<Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<*>>>>()
    private val nodeSelectionModeActionMapper = mock<NodeSelectionModeActionMapper>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val checkNodeCanBeMovedToTargetNode = mock<CheckNodeCanBeMovedToTargetNode>()
    private val nodeSelectionActionUiMapper = mock<NodeSelectionActionUiMapper>()
    private val mockRubbishNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(999L)
    }

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
        on { isTakenDown } doReturn false
    }

    private val mockNodeSelectionMenuItem = mock<NodeSelectionMenuItem<*>>()
    private val mockNodeSelectionModeMenuItem = mock<NodeSelectionModeMenuItem>()
    private val mockNodeSelectionAction = mock<NodeSelectionAction.Move>()

    @BeforeEach
    fun setUp() {
        whenever(cloudDriveOptions.get()).thenReturn(setOf(mockNodeSelectionMenuItem))
        getRubbishNodeUseCase.stub {
            onBlocking { invoke() } doReturn mockRubbishNode
        }
        nodeSelectionModeActionMapper.stub {
            onBlocking {
                invoke(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            } doReturn listOf(mockNodeSelectionModeMenuItem)
        }
        nodeSelectionActionUiMapper.stub {
            on { invoke(any()) } doReturn mockNodeSelectionAction
        }
        isNodeInBackupsUseCase.stub { onBlocking { invoke(any()) } doReturn false }
        getNodeAccessPermission.stub { onBlocking { invoke(any()) } doReturn AccessPermission.FULL }
        checkNodeCanBeMovedToTargetNode.stub { onBlocking { invoke(any(), any()) } doReturn true }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            cloudDriveOptions,
            nodeSelectionModeActionMapper,
            getRubbishNodeUseCase,
            isNodeInBackupsUseCase,
            getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode,
            nodeSelectionActionUiMapper
        )
    }

    private fun initViewModel() {
        underTest = NodeSelectionModeViewModel(
            cloudDriveOptions = cloudDriveOptions,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
            nodeSelectionActionUiMapper = nodeSelectionActionUiMapper
        )
    }

    @Test
    fun `test that view model initializes with empty state`() = runTest {
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().visibleActions).isEmpty()
        }
    }

    @Test
    fun `test that getRubbishBinNode handles failure case gracefully`() = runTest {
        whenever(getRubbishNodeUseCase()).thenThrow(RuntimeException("Test exception"))

        initViewModel()

        // Should not crash and should still initialize
        underTest.uiState.test {
            awaitItem()
        }
    }

    @Test
    fun `test updateState when node cannot be moved to rubbish bin`() = runTest {
        whenever(checkNodeCanBeMovedToTargetNode(any(), any())).thenReturn(false)

        initViewModel()

        val selectedNodes = setOf(mockFileNode)
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE

        underTest.updateState(selectedNodes, nodeSourceType)

        // Just ignore because we only want to verify the interactions
        underTest.uiState.test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(checkNodeCanBeMovedToTargetNode).invoke(mockFileNode.id, mockRubbishNode.id)
        verify(nodeSelectionModeActionMapper).invoke(
            options = setOf(mockNodeSelectionMenuItem),
            hasNodeAccessPermission = true,
            selectedNodes = selectedNodes.toList(),
            allNodeCanBeMovedToTarget = false, // Should be false when node cannot be moved to rubbish bin
            noNodeInBackups = true
        )
    }

    @Test
    fun `test state flow emits updated state after updateState call`() = runTest {
        initViewModel()

        val selectedNodes = setOf(mockFileNode)
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE

        underTest.uiState.test {
            // Initial state
            assertThat(awaitItem().visibleActions).isEmpty()

            // Update state
            underTest.updateState(selectedNodes, nodeSourceType)

            // Updated state
            val updatedState = awaitItem()
            assertThat(updatedState.visibleActions).isNotEmpty()
            assertThat(updatedState.visibleActions).contains(mockNodeSelectionAction)
        }
    }

    @Test
    fun `test updateState when node is in backups sets noNodeInBackups to false`() = runTest {
        val expected = true
        whenever(isNodeInBackupsUseCase(any())).thenReturn(expected)

        initViewModel()

        val selectedNodes = setOf(mockFileNode)
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE

        underTest.updateState(selectedNodes, nodeSourceType)

        // Just ignore because we only want to verify the interactions
        underTest.uiState.test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(nodeSelectionModeActionMapper).invoke(
            options = setOf(mockNodeSelectionMenuItem),
            hasNodeAccessPermission = true,
            selectedNodes = selectedNodes.toList(),
            allNodeCanBeMovedToTarget = true,
            noNodeInBackups = false // Should be false when node is in backups
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test updateState limits actions to 4 and adds More when there are more than 4 actions`() =
        runTest {
            // Create 5 mock actions to test the "More" functionality
            val mockAction1 = mock<NodeSelectionAction.Move> { on { testTag } doReturn "action1" }
            val mockAction2 = mock<NodeSelectionAction.Copy> { on { testTag } doReturn "action2" }
            val mockAction3 =
                mock<NodeSelectionAction.RubbishBin> { on { testTag } doReturn "action3" }
            val mockAction4 =
                mock<NodeSelectionAction.Download> { on { testTag } doReturn "action4" }
            val mockAction5 = mock<NodeSelectionAction.Hide> { on { testTag } doReturn "action5" }

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()

                )
            ).thenReturn(listOf(mock(), mock(), mock(), mock(), mock()))

            whenever(nodeSelectionActionUiMapper(any()))
                .thenReturn(
                    mockAction1,
                    mockAction2,
                    mockAction3,
                    mockAction4,
                    mockAction5
                )

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            underTest.uiState.test {
                awaitItem() // Initial state

                underTest.updateState(selectedNodes, nodeSourceType)

                val finalState = awaitItem() // Updated state

                // Should have exactly 5 items: first 4 actions + More
                assertThat(finalState.visibleActions).hasSize(5)
                assertThat(finalState.visibleActions[0]).isEqualTo(mockAction1)
                assertThat(finalState.visibleActions[1]).isEqualTo(mockAction2)
                assertThat(finalState.visibleActions[2]).isEqualTo(mockAction3)
                assertThat(finalState.visibleActions[3]).isEqualTo(mockAction4)
                assertThat(finalState.visibleActions[4]).isEqualTo(NodeSelectionAction.More)
            }
        }

    @Test
    fun `test updateState shows all actions when there are 4 or fewer actions`() = runTest {
        // Create 3 mock actions to test normal behavior
        val mockAction1 = mock<NodeSelectionAction.Move> { on { testTag } doReturn "action1" }
        val mockAction2 = mock<NodeSelectionAction.Copy> { on { testTag } doReturn "action2" }
        val mockAction3 =
            mock<NodeSelectionAction.RubbishBin> { on { testTag } doReturn "action3" }

        initViewModel()

        whenever(
            nodeSelectionModeActionMapper(
                options = any(),
                hasNodeAccessPermission = any(),
                selectedNodes = any(),
                allNodeCanBeMovedToTarget = any(),
                noNodeInBackups = any()
            )
        ).thenReturn(listOf(mock(), mock(), mock()))

        whenever(nodeSelectionActionUiMapper(any()))
            .thenReturn(
                mockAction1,
                mockAction2,
                mockAction3
            )

        val selectedNodes = setOf(mockFileNode)
        val nodeSourceType = NodeSourceType.CLOUD_DRIVE

        underTest.uiState.test {
            awaitItem() // Initial state

            underTest.updateState(selectedNodes, nodeSourceType)

            val finalState = awaitItem() // Updated state

            // Should have exactly 3 items: all actions, no More
            assertThat(finalState.visibleActions).hasSize(3)
            assertThat(finalState.visibleActions[0]).isEqualTo(mockAction1)
            assertThat(finalState.visibleActions[1]).isEqualTo(mockAction2)
            assertThat(finalState.visibleActions[2]).isEqualTo(mockAction3)
            assertThat(finalState.visibleActions).doesNotContain(NodeSelectionAction.More)
        }
    }
}
