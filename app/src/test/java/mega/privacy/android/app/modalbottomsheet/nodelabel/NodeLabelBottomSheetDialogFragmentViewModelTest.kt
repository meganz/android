package mega.privacy.android.app.modalbottomsheet.nodelabel

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLabelBottomSheetDialogFragmentViewModelTest {

    private lateinit var underTest: NodeLabelBottomSheetDialogFragmentViewModel

    private val updateNodeLabelUseCase: UpdateNodeLabelUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val analyticsTracker: AnalyticsTracker = mock()

    @BeforeEach
    fun setup() {
        reset(updateNodeLabelUseCase, getNodeByHandleUseCase, analyticsTracker)
        // Mock the static Analytics object
        Analytics.initialise(analyticsTracker)
        underTest = NodeLabelBottomSheetDialogFragmentViewModel(updateNodeLabelUseCase)
    }

    private fun createTestNode(
        id: Long = 12345L,
        label: Int = MegaNode.NODE_LBL_RED,
        name: String = "test_node",
    ): UnTypedNode {
        return mock<FolderNode> {
            on { this.id } doReturn NodeId(id)
            on { this.label } doReturn label
            on { this.name } doReturn name
        }
    }

    @Test
    fun `test updateNodeLabel with NodeId adds label successfully`() = runTest {
        // Given
        val nodeId = NodeId(12345L)
        val label = NodeLabel.RED
        whenever(updateNodeLabelUseCase(nodeId, label)).thenReturn(Unit)

        // When
        underTest.updateNodeLabel(nodeId, label)

        // Then
        verify(updateNodeLabelUseCase).invoke(nodeId, label)
    }

    @Test
    fun `test updateNodeLabel with NodeId removes label successfully`() = runTest {
        // Given
        val nodeId = NodeId(12345L)
        whenever(updateNodeLabelUseCase(nodeId, null)).thenReturn(Unit)

        // When
        underTest.updateNodeLabel(nodeId, null)

        // Then
        verify(updateNodeLabelUseCase).invoke(nodeId, null)
    }

    @Test
    fun `test updateNodeLabel with Long handle adds label successfully`() = runTest {
        // Given
        val nodeHandle = 12345L
        val label = NodeLabel.BLUE
        whenever(updateNodeLabelUseCase(NodeId(nodeHandle), label)).thenReturn(Unit)

        // When
        underTest.updateNodeLabel(nodeHandle, label)

        // Then
        verify(updateNodeLabelUseCase).invoke(NodeId(nodeHandle), label)
    }

    @Test
    fun `test updateMultipleNodeLabels with NodeId list adds labels successfully`() = runTest {
        // Given
        val nodeIds = listOf(NodeId(12345L), NodeId(67890L), NodeId(11111L))
        val label = NodeLabel.GREEN
        whenever(updateNodeLabelUseCase(any(), any())).thenReturn(Unit)

        // When
        underTest.updateMultipleNodeLabels(nodeIds, label)

        // Then
        nodeIds.forEach { nodeId ->
            verify(updateNodeLabelUseCase).invoke(nodeId, label)
        }
    }

    @Test
    fun `test updateMultipleNodeLabels with Long handles adds labels successfully`() = runTest {
        // Given
        val nodeHandles = listOf(12345L, 67890L, 11111L)
        val label = NodeLabel.PURPLE
        whenever(updateNodeLabelUseCase(any(), any())).thenReturn(Unit)

        // When
        underTest.updateMultipleNodeLabels(nodeHandles, label)


        // Then
        nodeHandles.forEach { handle ->
            verify(updateNodeLabelUseCase).invoke(NodeId(handle), label)
        }
    }

    @Test
    fun `test updateMultipleNodeLabels with empty list does not call use case`() = runTest {
        // Given
        val nodeHandles = emptyList<Long>()
        val label = NodeLabel.YELLOW

        // When
        underTest.updateMultipleNodeLabels(nodeHandles, label)


        // Then
        verify(updateNodeLabelUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `test getIntFromNodeLabel returns correct integer values`() {
        // Test all NodeLabel enum values
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.RED)).isEqualTo(MegaNode.NODE_LBL_RED)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.ORANGE)).isEqualTo(MegaNode.NODE_LBL_ORANGE)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.YELLOW)).isEqualTo(MegaNode.NODE_LBL_YELLOW)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.GREEN)).isEqualTo(MegaNode.NODE_LBL_GREEN)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.BLUE)).isEqualTo(MegaNode.NODE_LBL_BLUE)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.PURPLE)).isEqualTo(MegaNode.NODE_LBL_PURPLE)
        assertThat(underTest.getIntFromNodeLabel(NodeLabel.GREY)).isEqualTo(MegaNode.NODE_LBL_GREY)
    }

    @Test
    fun `test hasLabel returns true for nodes with labels`() {
        // Given
        val nodeWithRedLabel = mock<MegaNode> {
            on { label } doReturn MegaNode.NODE_LBL_RED
        }
        val nodeWithBlueLabel = mock<MegaNode> {
            on { label } doReturn MegaNode.NODE_LBL_BLUE
        }
        val nodeWithGreyLabel = mock<MegaNode> {
            on { label } doReturn MegaNode.NODE_LBL_GREY
        }

        // When & Then
        assertThat(underTest.hasLabel(nodeWithRedLabel)).isTrue()
        assertThat(underTest.hasLabel(nodeWithBlueLabel)).isTrue()
        assertThat(underTest.hasLabel(nodeWithGreyLabel)).isTrue()
    }

    @Test
    fun `test hasLabel returns false for nodes without labels`() {
        // Given
        val nodeWithoutLabel = mock<MegaNode> {
            on { label } doReturn MegaNode.NODE_LBL_UNKNOWN
        }
        val nodeWithInvalidLabel = mock<MegaNode> {
            on { label } doReturn -1
        }

        // When & Then
        assertThat(underTest.hasLabel(nodeWithoutLabel)).isFalse()
        assertThat(underTest.hasLabel(nodeWithInvalidLabel)).isFalse()
    }

    @Test
    fun `test getUniformLabel returns label when all nodes have same label`() {
        // Given
        val node1 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val node2 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val node3 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val nodes = listOf(node1, node2, node3)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isEqualTo(NodeLabel.RED)
    }

    @Test
    fun `test getUniformLabel returns null when nodes have different labels`() {
        // Given
        val node1 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val node2 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_BLUE }
        val node3 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_GREEN }
        val nodes = listOf(node1, node2, node3)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `test getUniformLabel returns null when some nodes have no label`() {
        // Given
        val node1 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val node2 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_RED }
        val node3 = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_UNKNOWN }
        val nodes = listOf(node1, node2, node3)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `test getUniformLabel returns null for empty list`() {
        // Given
        val nodes = emptyList<MegaNode>()

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `test getUniformLabel returns null for single node with no label`() {
        // Given
        val node = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_UNKNOWN }
        val nodes = listOf(node)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `test getUniformLabel returns label for single node with label`() {
        // Given
        val node = mock<MegaNode> { on { label } doReturn MegaNode.NODE_LBL_ORANGE }
        val nodes = listOf(node)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isEqualTo(NodeLabel.ORANGE)
    }

    @Test
    fun `test error handling when updateNodeLabelUseCase throws exception`() = runTest {
        // Given
        val nodeId = NodeId(12345L)
        val label = NodeLabel.RED
        val exception = RuntimeException("Network error")
        whenever(updateNodeLabelUseCase(nodeId, label)).thenThrow(exception)

        // When
        underTest.updateNodeLabel(nodeId, label)

        // Then
        verify(updateNodeLabelUseCase).invoke(nodeId, label)
        // The exception should be caught and logged, but not re-thrown
    }

    @Test
    fun `test error handling when updateMultipleNodeLabels throws exception`() = runTest {
        // Given
        val nodeIds = listOf(NodeId(12345L), NodeId(67890L))
        val label = NodeLabel.GREEN
        val exception = RuntimeException("API error")
        whenever(updateNodeLabelUseCase(any(), any())).thenThrow(exception)

        // When
        underTest.updateMultipleNodeLabels(nodeIds, label)


        // Then
        verify(updateNodeLabelUseCase).invoke(NodeId(12345L), label)
        // Analytics should not be tracked when the operation fails
        verify(analyticsTracker, never()).trackEvent(any())
    }

    @Test
    fun `test hasLabel with Node returns true for nodes with labels`() = runTest {
        // Given
        val nodeWithRedLabel = createTestNode(label = MegaNode.NODE_LBL_RED)
        val nodeWithBlueLabel = createTestNode(label = MegaNode.NODE_LBL_BLUE)

        // When & Then
        assertThat(underTest.hasLabel(nodeWithRedLabel)).isTrue()
        assertThat(underTest.hasLabel(nodeWithBlueLabel)).isTrue()
    }

    @Test
    fun `test hasLabel with Node returns false for nodes without labels`() = runTest {
        // Given
        val nodeWithoutLabel = createTestNode(label = MegaNode.NODE_LBL_UNKNOWN)

        // When & Then
        assertThat(underTest.hasLabel(nodeWithoutLabel)).isFalse()
    }

    @Test
    fun `test getUniformLabel with Node returns label when all nodes have same label`() = runTest {
        // Given
        val node1 = createTestNode(id = 1L, label = MegaNode.NODE_LBL_RED)
        val node2 = createTestNode(id = 2L, label = MegaNode.NODE_LBL_RED)
        val node3 = createTestNode(id = 3L, label = MegaNode.NODE_LBL_RED)
        val nodes = listOf(node1, node2, node3)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isEqualTo(NodeLabel.RED)
    }

    @Test
    fun `test getUniformLabel with Node returns null when nodes have different labels`() = runTest {
        // Given
        val node1 = createTestNode(id = 1L, label = MegaNode.NODE_LBL_RED)
        val node2 = createTestNode(id = 2L, label = MegaNode.NODE_LBL_BLUE)
        val node3 = createTestNode(id = 3L, label = MegaNode.NODE_LBL_GREEN)
        val nodes = listOf(node1, node2, node3)

        // When
        val result = underTest.getUniformLabel(nodes)

        // Then
        assertThat(result).isNull()
    }
}
