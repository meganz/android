package mega.privacy.android.app.modalbottomsheet.nodelabel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.system.measureTimeMillis

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLabelBottomSheetDialogFragmentViewModelV2Test {

    private lateinit var underTest: NodeLabelBottomSheetDialogFragmentViewModelV2

    private val updateNodeLabelUseCase: UpdateNodeLabelUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(updateNodeLabelUseCase, getNodeByIdUseCase)
        underTest = NodeLabelBottomSheetDialogFragmentViewModelV2(
            updateNodeLabelUseCase = updateNodeLabelUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase
        )
    }

    private fun createTypedNodeMock(nodeLabel: NodeLabel? = null): TypedNode {
        return mock<TypedNode>().apply {
            whenever(this.nodeLabel).thenReturn(nodeLabel)
        }
    }

    @Test
    fun `test that initial UI state is correct`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.node).isNull()
            assertThat(initialState.nodes).isEmpty()
            assertThat(initialState.isMultipleSelection).isFalse()
            assertThat(initialState.shouldDismiss).isFalse()
            assertThat(initialState.hasError).isFalse()
            assertThat(initialState.hasNodes).isFalse()
        }
    }

    @Test
    fun `test that getUniformLabel returns correct label when all nodes have same label`() =
        runTest {
            val mockNode1 = createTypedNodeMock(NodeLabel.RED)
            val mockNode2 = createTypedNodeMock(NodeLabel.RED)
            val mockNode3 = createTypedNodeMock(NodeLabel.RED)

            val nodes = listOf(mockNode1, mockNode2, mockNode3)
            val result = underTest.getUniformLabel(nodes)

            assertThat(result).isEqualTo(NodeLabel.RED)
        }

    @Test
    fun `test that getUniformLabel returns null when nodes have different labels`() = runTest {
        val mockNode1 = createTypedNodeMock(NodeLabel.RED)
        val mockNode2 = createTypedNodeMock(NodeLabel.BLUE)

        val nodes = listOf(mockNode1, mockNode2)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    @Test
    fun `test that getUniformLabel returns null when nodes have no labels`() = runTest {
        val mockNode1 = createTypedNodeMock(null)
        val mockNode2 = createTypedNodeMock(null)

        val nodes = listOf(mockNode1, mockNode2)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    @Test
    fun `test that getUniformLabel returns null for empty list`() = runTest {
        val result = underTest.getUniformLabel(emptyList())
        assertThat(result).isNull()
    }

    @Test
    fun `test that hasLabel returns true for labeled nodes`() = runTest {
        val mockNode = createTypedNodeMock(NodeLabel.RED)

        val result = underTest.hasLabel(mockNode)
        assertThat(result).isTrue()
    }

    @Test
    fun `test that hasLabel returns false for unlabeled nodes`() = runTest {
        val mockNode = createTypedNodeMock(null)

        val result = underTest.hasLabel(mockNode)
        assertThat(result).isFalse()
    }

    @Test
    fun `test that getUniformLabel handles unknown label values`() = runTest {
        val mockNode1 = createTypedNodeMock(null) // Unknown label
        val mockNode2 = createTypedNodeMock(NodeLabel.RED)

        val nodes = listOf(mockNode1, mockNode2)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    @Test
    fun `test that getUniformLabel handles single node with unknown label`() = runTest {
        val mockNode = createTypedNodeMock(null) // Unknown label

        val nodes = listOf(mockNode)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    @Test
    fun `test that getUniformLabel handles all label types correctly`() = runTest {
        val labels = listOf(
            NodeLabel.RED,
            NodeLabel.ORANGE,
            NodeLabel.YELLOW,
            NodeLabel.GREEN,
            NodeLabel.BLUE,
            NodeLabel.PURPLE,
            NodeLabel.GREY
        )

        labels.forEach { expectedLabel ->
            val mockNode1 = createTypedNodeMock(expectedLabel)
            val mockNode2 = createTypedNodeMock(expectedLabel)

            val nodes = listOf(mockNode1, mockNode2)
            val result = underTest.getUniformLabel(nodes)

            assertThat(result).isEqualTo(expectedLabel)
        }
    }

    // MARK: - Load Node Tests

    @Test
    fun `test that loadNode sets loading state and loads node successfully`() = runTest {
        val nodeHandle = 123L
        val mockNode = createTypedNodeMock(NodeLabel.RED)
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(mockNode)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNode(nodeHandle)
            advanceUntilIdle()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.node).isEqualTo(mockNode)
            assertThat(successState.isMultipleSelection).isFalse()
            assertThat(successState.hasError).isFalse()
            assertThat(successState.hasNodes).isTrue()
        }

        // Verify that the use case was called correctly
        verify(getNodeByIdUseCase, times(1)).invoke(NodeId(nodeHandle))
        verifyNoMoreInteractions(getNodeByIdUseCase)
        verifyNoInteractions(updateNodeLabelUseCase)
    }

    @Test
    fun `test that loadNode handles errors correctly`() = runTest {
        val nodeHandle = 123L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Node not found"))

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNode(nodeHandle)
            advanceUntilIdle()

            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.hasError).isTrue()
            assertThat(errorState.shouldDismiss).isTrue()
        }
    }

    // MARK: - Load Nodes Tests

    @Test
    fun `test that loadNodes sets loading state and loads multiple nodes successfully`() = runTest {
        val nodeHandles = longArrayOf(123L, 456L, 789L)
        val mockNode1 = createTypedNodeMock(NodeLabel.RED)
        val mockNode2 = createTypedNodeMock(NodeLabel.BLUE)
        val mockNode3 = createTypedNodeMock(NodeLabel.GREEN)

        whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(mockNode1)
        whenever(getNodeByIdUseCase(NodeId(456L))).thenReturn(mockNode2)
        whenever(getNodeByIdUseCase(NodeId(789L))).thenReturn(mockNode3)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNodes(nodeHandles)

            // Get the loading state (emitted immediately when setLoadingState() is called)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Advance until the coroutine completes
            advanceUntilIdle()

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.nodes).hasSize(3)
            assertThat(successState.nodes).containsExactly(mockNode1, mockNode2, mockNode3)
            assertThat(successState.isMultipleSelection).isTrue()
            assertThat(successState.hasError).isFalse()
            assertThat(successState.hasNodes).isTrue()
        }
    }

    @Test
    fun `test that loadNodes handles partial failures correctly`() = runTest {
        val nodeHandles = longArrayOf(123L, 456L, 789L)
        val mockNode1 = createTypedNodeMock(NodeLabel.RED)
        val mockNode3 = createTypedNodeMock(NodeLabel.GREEN)

        whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(mockNode1)
        whenever(getNodeByIdUseCase(NodeId(456L))).thenThrow(RuntimeException("Node not found"))
        whenever(getNodeByIdUseCase(NodeId(789L))).thenReturn(mockNode3)

        underTest.loadNodes(nodeHandles)

        underTest.uiState.test {
            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.nodes).hasSize(0)
            assertThat(successState.hasError).isTrue()
        }
    }

    @Test
    fun `test that loadNodes handles all failures correctly`() = runTest {
        val nodeHandles = longArrayOf(123L, 456L)
        whenever(getNodeByIdUseCase(any())).thenThrow(RuntimeException("All nodes failed"))

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNodes(nodeHandles)

            // Get the loading state (emitted immediately when setLoadingState() is called)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Advance until the coroutine completes
            advanceUntilIdle()

            // Get the final error state
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.hasError).isTrue()
            assertThat(errorState.shouldDismiss).isTrue()
        }
    }

    // MARK: - Update Node Label Tests

    @Test
    fun `test that updateNodeLabel sets loading state and updates label successfully`() = runTest {
        val nodeHandle = 123L
        val label = NodeLabel.RED
        whenever(updateNodeLabelUseCase(NodeId(nodeHandle), label)).thenReturn(Unit)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateNodeLabel(nodeHandle, label)

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.shouldDismiss).isTrue()
            assertThat(successState.hasError).isFalse()
        }

        // Verify that the use case was called correctly
        verify(updateNodeLabelUseCase, times(1)).invoke(NodeId(nodeHandle), label)
        verifyNoMoreInteractions(updateNodeLabelUseCase)
        verifyNoInteractions(getNodeByIdUseCase)
    }

    @Test
    fun `test that updateNodeLabel handles errors correctly`() = runTest {
        val nodeHandle = 123L
        val label = NodeLabel.RED
        whenever(updateNodeLabelUseCase(NodeId(nodeHandle), label))
            .thenThrow(RuntimeException("Update failed"))

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateNodeLabel(nodeHandle, label)

            // Get the final error state
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.hasError).isTrue()
            assertThat(errorState.shouldDismiss).isTrue()
        }
    }

    @Test
    fun `test that updateNodeLabel removes label when null is passed`() = runTest {
        val nodeHandle = 123L
        whenever(updateNodeLabelUseCase(NodeId(nodeHandle), null)).thenReturn(Unit)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateNodeLabel(nodeHandle, null)

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.shouldDismiss).isTrue()
        }
    }

    // MARK: - Update Multiple Node Labels Tests

    @Test
    fun `test that updateMultipleNodeLabels sets loading state and updates all labels successfully`() =
        runTest {
            val nodeHandles = listOf(123L, 456L, 789L)
            val label = NodeLabel.BLUE
            whenever(updateNodeLabelUseCase(any(), any())).thenReturn(Unit)

            underTest.uiState.test {
                // Skip the initial state
                awaitItem()

                underTest.updateMultipleNodeLabels(nodeHandles, label)

                // Get the loading state (emitted immediately when setLoadingState() is called)
                val loadingState = awaitItem()
                assertThat(loadingState.isLoading).isTrue()

                // Advance until the coroutine completes
                advanceUntilIdle()

                // Get the final success state
                val successState = awaitItem()
                assertThat(successState.isLoading).isFalse()
                assertThat(successState.shouldDismiss).isTrue()
                assertThat(successState.hasError).isFalse()
            }
        }

    @Test
    fun `test that updateMultipleNodeLabels handles errors correctly`() = runTest {
        val nodeHandles = listOf(123L, 456L)
        val label = NodeLabel.GREEN
        whenever(updateNodeLabelUseCase(any(), any()))
            .thenThrow(RuntimeException("Update failed"))

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateMultipleNodeLabels(nodeHandles, label)

            // Get the loading state (emitted immediately when setLoadingState() is called)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Advance until the coroutine completes
            advanceUntilIdle()

            // Get the final error state
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.hasError).isTrue()
            assertThat(errorState.shouldDismiss).isTrue()
        }
    }

    @Test
    fun `test that updateMultipleNodeLabels removes labels when null is passed`() = runTest {
        val nodeHandles = listOf(123L, 456L)
        whenever(updateNodeLabelUseCase(any(), any())).thenReturn(Unit)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateMultipleNodeLabels(nodeHandles, null)
            advanceUntilIdle()

            // Get the loading state
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.shouldDismiss).isTrue()
        }
    }

    // MARK: - State Management Tests

    @Test
    fun `test that hasNodes returns true when single node is loaded`() = runTest {
        val mockNode = createTypedNodeMock(NodeLabel.RED)
        whenever(getNodeByIdUseCase(any())).thenReturn(mockNode)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNode(123L)

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.hasNodes).isTrue()
        }
    }

    @Test
    fun `test that hasNodes returns true when multiple nodes are loaded`() = runTest {
        val mockNode1 = createTypedNodeMock(NodeLabel.RED)
        val mockNode2 = createTypedNodeMock(NodeLabel.BLUE)
        whenever(getNodeByIdUseCase(any())).thenReturn(mockNode1, mockNode2)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNodes(longArrayOf(123L, 456L))

            // Get the loading state (emitted immediately when setLoadingState() is called)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Advance until the coroutine completes
            advanceUntilIdle()

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.hasNodes).isTrue()
        }
    }

    @Test
    fun `test that hasNodes returns false when no nodes are loaded`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.hasNodes).isFalse()
        }
    }

    // MARK: - Edge Cases and Error Scenarios

    @Test
    fun `test that loadNodes handles empty array correctly`() = runTest {
        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNodes(longArrayOf())

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.nodes).isEmpty()
            assertThat(successState.isMultipleSelection).isTrue()
            assertThat(successState.hasError).isFalse()
            assertThat(successState.hasNodes).isFalse()
        }
    }

    @Test
    fun `test that updateMultipleNodeLabels handles empty list correctly`() = runTest {
        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateMultipleNodeLabels(emptyList(), NodeLabel.RED)

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.shouldDismiss).isTrue()
            assertThat(successState.hasError).isFalse()
        }
    }

    @Test
    fun `test that getUniformLabel handles mixed null and labeled nodes`() = runTest {
        val mockNode1 = createTypedNodeMock(NodeLabel.RED)
        val mockNode2 = createTypedNodeMock(null)
        val mockNode3 = createTypedNodeMock(NodeLabel.RED)

        val nodes = listOf(mockNode1, mockNode2, mockNode3)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    @Test
    fun `test that getUniformLabel handles single node correctly`() = runTest {
        val mockNode = createTypedNodeMock(NodeLabel.BLUE)

        val nodes = listOf(mockNode)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isEqualTo(NodeLabel.BLUE)
    }

    @Test
    fun `test that getUniformLabel handles single node with null label`() = runTest {
        val mockNode = createTypedNodeMock(null)

        val nodes = listOf(mockNode)
        val result = underTest.getUniformLabel(nodes)

        assertThat(result).isNull()
    }

    // MARK: - State Management Edge Cases

    @Test
    fun `test that hasNodes correctly handles state transitions between single and multiple selection`() =
        runTest {
            // Start with single node
            val singleNode = createTypedNodeMock(NodeLabel.RED)
            whenever(getNodeByIdUseCase(any())).thenReturn(singleNode)

            underTest.uiState.test {
                // Skip the initial state
                awaitItem()

                underTest.loadNode(1L)

                // Get the final success state
                val singleState = awaitItem()
                assertThat(singleState.hasNodes).isTrue()
                assertThat(singleState.isMultipleSelection).isFalse()
            }

            // Switch to multiple nodes
            val multipleNodes = listOf(
                createTypedNodeMock(NodeLabel.RED),
                createTypedNodeMock(NodeLabel.BLUE)
            )
            whenever(getNodeByIdUseCase(any())).thenReturn(multipleNodes[0], multipleNodes[1])

            underTest.uiState.test {
                // Skip the initial state
                awaitItem()

                underTest.loadNodes(longArrayOf(2L, 3L))
                advanceUntilIdle()

                // Get the loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoading).isTrue()

                // Get the final success state
                val multipleState = awaitItem()
                assertThat(multipleState.hasNodes).isTrue()
                assertThat(multipleState.isMultipleSelection).isTrue()
                assertThat(multipleState.nodes).hasSize(2)
            }
        }

    // MARK: - Integration-Style Tests

    @Test
    fun `test that complete workflow from loading to updating multiple nodes`() = runTest {
        // Step 1: Load multiple nodes
        val nodeHandles = longArrayOf(1L, 2L, 3L)
        val mockNodes = listOf(
            createTypedNodeMock(NodeLabel.RED),
            createTypedNodeMock(NodeLabel.BLUE),
            createTypedNodeMock(NodeLabel.GREEN)
        )

        nodeHandles.forEachIndexed { index, handle ->
            whenever(getNodeByIdUseCase(NodeId(handle))).thenReturn(mockNodes[index])
        }

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.loadNodes(nodeHandles)
            advanceUntilIdle()

            // Get the loading state
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.nodes).hasSize(3)
            assertThat(successState.isMultipleSelection).isTrue()
        }

        // Step 2: Update all nodes with new label
        val newLabel = NodeLabel.PURPLE
        whenever(updateNodeLabelUseCase(any(), any())).thenReturn(Unit)

        underTest.uiState.test {
            // Skip the initial state
            awaitItem()

            underTest.updateMultipleNodeLabels(nodeHandles.toList(), newLabel)
            advanceUntilIdle()

            // Get the loading state
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Get the final success state
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.shouldDismiss).isTrue()
        }

        // Verify all operations were called correctly
        verify(getNodeByIdUseCase, times(3)).invoke(any())
        verify(updateNodeLabelUseCase, times(3)).invoke(any(), eq(newLabel))
    }
}
