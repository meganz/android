package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAncestorsIdsUseCaseTest {
    private lateinit var underTest: GetAncestorsIdsUseCase
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetAncestorsIdsUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that empty list is returned when node has no parent`() = runTest {
        val node = mock<Node> {
            whenever(it.parentId).thenReturn(NodeId(-1L))
        }

        val actual = underTest(node)

        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that list with single ancestor is returned when node has one parent`() = runTest {
        val parentId = NodeId(456L)
        val node = mock<Node> {
            whenever(it.parentId).thenReturn(parentId)
        }

        whenever(nodeRepository.getParentNodeId(parentId)).thenReturn(null)

        val actual = underTest(node)

        assertThat(actual).containsExactly(parentId)
    }

    @Test
    fun `test that list with single ancestor is returned when parent is root node`() = runTest {
        val parentId = NodeId(456L)
        val node = mock<Node> {
            whenever(it.parentId).thenReturn(parentId)
        }

        whenever(nodeRepository.getParentNodeId(parentId)).thenReturn(NodeId(-1L))

        val actual = underTest(node)

        assertThat(actual).containsExactly(parentId)
    }

    @Test
    fun `test that list with multiple ancestors is returned when node has parent chain`() =
        runTest {
            val parentId = NodeId(456L)
            val grandParentId = NodeId(789L)
            val rootId = NodeId(-1)
            val node = mock<Node> {
                whenever(it.parentId).thenReturn(parentId)
            }

            whenever(nodeRepository.getParentNodeId(parentId)).thenReturn(grandParentId)
            whenever(nodeRepository.getParentNodeId(grandParentId)).thenReturn(rootId)

            val actual = underTest(node)

            assertThat(actual).containsExactly(parentId, grandParentId).inOrder()
        }
}

