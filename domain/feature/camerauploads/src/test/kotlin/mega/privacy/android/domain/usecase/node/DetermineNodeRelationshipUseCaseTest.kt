package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.HasAncestor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DetermineNodeRelationshipUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()
    private val hasAncestor = mock<HasAncestor>()

    private lateinit var underTest: DetermineNodeRelationshipUseCase

    @BeforeEach
    fun setUp() {
        underTest = DetermineNodeRelationshipUseCase(nodeRepository, hasAncestor)
        reset(nodeRepository, hasAncestor)
    }

    @Test
    fun `test that exact match returns ExactMatch`() = runTest {
        val nodeId = NodeId(123L)

        val result = underTest(nodeId, nodeId)

        assertThat(result).isEqualTo(NodeRelationship.ExactMatch)
        verifyNoInteractions(nodeRepository, hasAncestor)
    }

    @Test
    fun `test that source is subfolder of target returns TargetIsAncestor`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Node hierarchy: /Photos (target: id=2) -> /Photos/Vacation (source: id=1)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos/Vacation")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsAncestor)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that source is grandchild of target returns TargetIsAncestor`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Node hierarchy: /Photos (target: id=2) -> /Photos/Vacation -> /Photos/Vacation/2023 (source: id=1)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos/Vacation/2023")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsAncestor)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that target is subfolder of source returns TargetIsDescendant`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Node hierarchy: /Photos (source: id=1) -> /Photos/Vacation (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos/Vacation")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsDescendant)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that target is grandchild of source returns TargetIsDescendant`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Node hierarchy: /Photos (source: id=1) -> /Photos/Vacation -> /Photos/Vacation/2023 (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos/Vacation/2023")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsDescendant)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that nodes in different branches return NoMatch`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Different branches: /Documents (source: id=1) and /Photos (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Documents")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.NoMatch)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that similar path names but not subpath return NoMatch`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Similar names but not parent/child: /DocumentsWork (source: id=1) and /Documents (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/DocumentsWork")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Documents")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.NoMatch)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that paths with trailing slashes are handled correctly`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Paths with trailing slashes: /Photos/ (source: id=1) and /Photos/ (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos/")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos/")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.ExactMatch)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that paths with mixed trailing slashes work correctly`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Mixed trailing slashes: /Photos (source: id=1) is parent of /Photos/Vacation/ (target: id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos/Vacation/")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsDescendant)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that when source path is null, parent chain traversal is used`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Source node path is null (e.g., in rubbish bin)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn(null)
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Photos")
        whenever(hasAncestor(sourceNodeId, targetNodeId)).thenReturn(true)

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsAncestor)
    }

    @Test
    fun `test that when target path is null, parent chain traversal is used`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Target node path is null (e.g., in rubbish bin)
        // Node hierarchy: /Photos (source: id=1) is parent of target (id=2)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Photos")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn(null)
        // hasAncestor(targetNodeId, sourceNodeId) = true means:
        // "sourceNodeId (1) is in the ancestor chain of targetNodeId (2)"
        // Therefore target is descendant of source -> TargetIsDescendant
        whenever(hasAncestor(targetNodeId, sourceNodeId)).thenReturn(true)
        whenever(hasAncestor(sourceNodeId, targetNodeId)).thenReturn(false)

        val result = underTest(sourceNodeId, targetNodeId)

        // Since source is ancestor of target, result is TargetIsDescendant
        assertThat(result).isEqualTo(NodeRelationship.TargetIsDescendant)
    }

    @Test
    fun `test that when both paths are null, parent chain traversal is used`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Both paths are null (e.g., both in rubbish bin)
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn(null)
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn(null)
        whenever(hasAncestor(sourceNodeId, targetNodeId)).thenReturn(false)
        whenever(hasAncestor(targetNodeId, sourceNodeId)).thenReturn(true)

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsDescendant)
    }

    @Test
    fun `test that NoMatch is returned when parent chain traversal finds no relationship`() =
        runTest {
            val sourceNodeId = NodeId(1L)
            val targetNodeId = NodeId(2L)
            // Both paths are null and no ancestor relationship
            whenever(nodeRepository.getFullNodePathById(sourceNodeId))
                .thenReturn(null)
            whenever(nodeRepository.getFullNodePathById(targetNodeId))
                .thenReturn(null)
            whenever(hasAncestor(sourceNodeId, targetNodeId)).thenReturn(false)
            whenever(hasAncestor(targetNodeId, sourceNodeId)).thenReturn(false)

            val result = underTest(sourceNodeId, targetNodeId)

            assertThat(result).isEqualTo(NodeRelationship.NoMatch)
        }

    @Test
    fun `test that NoMatch is returned when parent chain traversal throws exception`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // hasAncestor throws exception
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn(null)
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn(null)
        whenever(hasAncestor(sourceNodeId, targetNodeId))
            .thenThrow(RuntimeException("Test exception"))

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.NoMatch)
    }

    @Test
    fun `test that cloud drive paths are handled correctly`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Cloud Drive hierarchy
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("/Cloud Drive/Documents/Work")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Cloud Drive/Documents")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.TargetIsAncestor)
        verifyNoInteractions(hasAncestor)
    }

    @Test
    fun `test that incoming shares path returns NoMatch with cloud drive path`() = runTest {
        val sourceNodeId = NodeId(1L)
        val targetNodeId = NodeId(2L)
        // Different locations: Cloud Drive and Incoming Shares
        whenever(nodeRepository.getFullNodePathById(sourceNodeId))
            .thenReturn("//bin/Incoming Shares/SharedFolder")
        whenever(nodeRepository.getFullNodePathById(targetNodeId))
            .thenReturn("/Cloud Drive/Documents")

        val result = underTest(sourceNodeId, targetNodeId)

        assertThat(result).isEqualTo(NodeRelationship.NoMatch)
        verifyNoInteractions(hasAncestor)
    }
}
