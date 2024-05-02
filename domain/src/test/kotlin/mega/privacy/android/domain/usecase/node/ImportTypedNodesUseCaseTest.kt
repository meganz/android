package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImportNodesResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.namecollision.TypedNodeNameCollisionResult
import mega.privacy.android.domain.usecase.node.namecollision.CheckTypedNodeNameCollisionUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportTypedNodesUseCaseTest {

    private lateinit var underTest: ImportTypedNodesUseCase

    private val checkTypedNodeNameCollisionUseCase = mock<CheckTypedNodeNameCollisionUseCase>()
    private val copyTypedNodeUseCase = mock<CopyTypedNodeUseCase>()

    private val handleWhereToImport = 123L
    private val parentNodeId = NodeId(handleWhereToImport)
    private val nodeId = NodeId(0)
    private val node1 = mock<TypedNode> {
        on { name } doReturn "node1"
    }
    private val collision1 = NodeNameCollision.Default(
        collisionHandle = 0,
        nodeHandle = 0,
        parentHandle = 0,
        name = node1.name,
        size = 0,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0,
        isFile = false
    )
    private val node2 = mock<TypedNode> {
        on { name } doReturn "node2"
    }
    private val collision2 = NodeNameCollision.Default(
        collisionHandle = 0,
        nodeHandle = 0,
        parentHandle = 0,
        name = node2.name,
        size = 0,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0,
        isFile = false
    )
    private val node3 = mock<TypedNode> {
        on { name } doReturn "node3"
    }
    private val collision3 = NodeNameCollision.Default(
        collisionHandle = 0,
        nodeHandle = 0,
        parentHandle = 0,
        name = node3.name,
        size = 0,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0,
        isFile = false
    )
    private val nodes = listOf(node1, node2, node3)

    @BeforeAll
    fun setUp() {
        underTest = ImportTypedNodesUseCase(
            checkTypedNodeNameCollisionUseCase,
            copyTypedNodeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkTypedNodeNameCollisionUseCase,
            copyTypedNodeUseCase
        )
    }

    @Test
    fun `test that returns correctly when there are no collisions and all the nodes are successfully copied`() =
        runTest {
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(nodes, emptyList())
            )
            nodes.forEach { node ->
                whenever(copyTypedNodeUseCase(node, parentNodeId)).thenReturn(nodeId)
            }

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(nodes.size, 0, emptyList()))
        }

    @Test
    fun `test that returns correctly when there are no collisions and one of the nodes was not copied`() =
        runTest {
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(nodes, emptyList())
            )
            whenever(copyTypedNodeUseCase(node1, parentNodeId)).thenReturn(nodeId)
            whenever(copyTypedNodeUseCase(node2, parentNodeId)).thenAnswer { throw Exception() }
            whenever(copyTypedNodeUseCase(node3, parentNodeId)).thenReturn(nodeId)

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(2, 1, emptyList()))
        }

    @Test
    fun `test that returns correctly when there are no collisions and none of the nodes were copied`() =
        runTest {
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(nodes, emptyList())
            )
            nodes.forEach { node ->
                whenever(copyTypedNodeUseCase(node, parentNodeId)).thenAnswer { throw Exception() }
            }

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(0, nodes.size, emptyList()))
        }

    @Test
    fun `test that returns correctly when there is a collision and the rest of the nodes were successfully copied`() =
        runTest {
            val collisions = listOf(collision3)
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(listOf(node1, node2), collisions)
            )
            whenever(copyTypedNodeUseCase(node1, parentNodeId)).thenReturn(nodeId)
            whenever(copyTypedNodeUseCase(node2, parentNodeId)).thenReturn(nodeId)

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(2, 0, collisions))
        }

    @Test
    fun `test that returns correctly when all the nodes have collisions`() =
        runTest {
            val collisions = listOf(collision1, collision2, collision3)
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(emptyList(), collisions)
            )

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(0, 0, collisions))
        }

    @Test
    fun `test that returns correctly when there is a collision and one of the nodes was not copied`() =
        runTest {
            val collisions = listOf(collision3)
            whenever(checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport)).thenReturn(
                TypedNodeNameCollisionResult(listOf(node1, node2), collisions)
            )
            whenever(copyTypedNodeUseCase(node1, parentNodeId)).thenReturn(nodeId)
            whenever(copyTypedNodeUseCase(node2, parentNodeId)).thenAnswer { throw Exception() }

            Truth.assertThat(underTest(nodes, handleWhereToImport))
                .isEqualTo(ImportNodesResult(1, 1, collisions))
        }
}