package mega.privacy.android.domain.usecase.node.namecollision

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.namecollision.TypedNodeNameCollisionResult
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckTypedNodeNameCollisionUseCaseTest {

    private lateinit var underTest: CheckTypedNodeNameCollisionUseCase

    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()

    private val handleWhereToImport = 123L
    private val node1 = mock<TypedNode> {
        on { name } doReturn "node1"
    }
    private val collision1 = NodeNameCollision(
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
    private val collision2 = NodeNameCollision(
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
    private val collision3 = NodeNameCollision(
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
    private val folderNode = mock<FolderNode>()
    private val fileNode = mock<FileNode>()

    @BeforeAll
    fun setUp() {
        underTest = CheckTypedNodeNameCollisionUseCase(
            isNodeInRubbishBinUseCase,
            getChildNodeUseCase,
            getNodeByHandleUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbishBinUseCase,
            getChildNodeUseCase,
            getNodeByHandleUseCase,
        )
    }

    @Test
    fun `test that all nodes do not have conflict when parent node is null`() = runTest {
        whenever(getNodeByHandleUseCase(handleWhereToImport)).thenReturn(null)

        Truth.assertThat(underTest.invoke(nodes, handleWhereToImport))
            .isEqualTo(TypedNodeNameCollisionResult(nodes, emptyList()))
    }

    @Test
    fun `test that all nodes do not have conflict when parent node is in Rubbish Bin`() = runTest {
        whenever(getNodeByHandleUseCase(handleWhereToImport)).thenReturn(folderNode)
        whenever(isNodeInRubbishBinUseCase(NodeId(handleWhereToImport))).thenReturn(true)

        Truth.assertThat(underTest.invoke(nodes, handleWhereToImport))
            .isEqualTo(TypedNodeNameCollisionResult(nodes, emptyList()))
    }

    @Test
    fun `test that returns correctly when all the nodes have conflicts`() = runTest {
        whenever(getNodeByHandleUseCase(handleWhereToImport)).thenReturn(folderNode)
        whenever(isNodeInRubbishBinUseCase(NodeId(handleWhereToImport))).thenReturn(false)
        nodes.forEach { node ->
            whenever(getChildNodeUseCase(NodeId(handleWhereToImport), node.name))
                .thenReturn(fileNode)
        }

        Truth.assertThat(underTest.invoke(nodes, handleWhereToImport))
            .isEqualTo(
                TypedNodeNameCollisionResult(
                    emptyList(),
                    listOf(collision1, collision2, collision3)
                )
            )
    }

    @Test
    fun `test that returns correctly when one of the nodes has conflict`() = runTest {
        whenever(getNodeByHandleUseCase(handleWhereToImport)).thenReturn(folderNode)
        whenever(isNodeInRubbishBinUseCase(NodeId(handleWhereToImport))).thenReturn(false)
        whenever(getChildNodeUseCase(NodeId(handleWhereToImport), node1.name))
            .thenReturn(null)
        whenever(getChildNodeUseCase(NodeId(handleWhereToImport), node2.name))
            .thenReturn(fileNode)
        whenever(getChildNodeUseCase(NodeId(handleWhereToImport), node3.name))
            .thenReturn(null)

        Truth.assertThat(underTest.invoke(nodes, handleWhereToImport))
            .isEqualTo(
                TypedNodeNameCollisionResult(
                    listOf(node1, node3),
                    listOf(collision2)
                )
            )
    }
}
