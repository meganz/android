package mega.privacy.android.domain.usecase.node.publiclink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckPublicNodesNameCollisionUseCaseTest {
    private lateinit var underTest: CheckPublicNodesNameCollisionUseCase

    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    private val targetHandle = 10L
    private val nodeHandle = 1L
    private val name = "file.txt"

    @BeforeAll
    fun setUp() {
        underTest = CheckPublicNodesNameCollisionUseCase(
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getChildNodeUseCase = getChildNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbishBinUseCase,
            getChildNodeUseCase,
            getNodeByHandleUseCase,
            getRootNodeUseCase,
            nodeRepository,
        )
    }

    @Test
    fun `test that invoke returns a file link collision carrying the serialized data when a child with the same name exists`() =
        runTest {
            stubTargetFolder()
            val conflictNode = mock<FileNode> {
                on { id } doReturn NodeId(20L)
            }
            whenever(getChildNodeUseCase(NodeId(targetHandle), name)).thenReturn(conflictNode)
            val node = publicFileNode(serializedData = "blob")

            val result = underTest(
                nodes = listOf(node),
                targetHandle = targetHandle,
                type = NodeNameCollisionType.COPY,
            )

            assertThat(result.noConflictNodes).isEmpty()
            assertThat(result.type).isEqualTo(NodeNameCollisionType.COPY)
            val collision = result.conflictNodes.single() as NodeNameCollision.FileLink
            assertThat(collision.collisionHandle).isEqualTo(20L)
            assertThat(collision.nodeHandle).isEqualTo(nodeHandle)
            assertThat(collision.parentHandle).isEqualTo(targetHandle)
            assertThat(collision.serializedData).isEqualTo("blob")
            assertThat(collision.isFile).isTrue()
        }

    @Test
    fun `test that invoke returns the node without conflict when no child with the same name exists`() =
        runTest {
            stubTargetFolder()
            whenever(getChildNodeUseCase(NodeId(targetHandle), name)).thenReturn(null)
            val node = publicFileNode(serializedData = "blob")

            val result = underTest(
                nodes = listOf(node),
                targetHandle = targetHandle,
                type = NodeNameCollisionType.COPY,
            )

            assertThat(result.noConflictNodes).containsExactly(node)
            assertThat(result.conflictNodes).isEmpty()
        }

    @Test
    fun `test that invoke throws when a colliding public node has no serialized data`() = runTest {
        stubTargetFolder()
        val conflictNode = mock<FileNode> {
            on { id } doReturn NodeId(20L)
        }
        whenever(getChildNodeUseCase(NodeId(targetHandle), name)).thenReturn(conflictNode)
        val node = publicFileNode(serializedData = null)

        assertThrows<IllegalArgumentException> {
            underTest(
                nodes = listOf(node),
                targetHandle = targetHandle,
                type = NodeNameCollisionType.COPY,
            )
        }
    }

    private suspend fun stubTargetFolder() {
        val targetFolder = mock<FolderNode> {
            on { id } doReturn NodeId(targetHandle)
            on { childFolderCount } doReturn 0
            on { childFileCount } doReturn 1
        }
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(isNodeInRubbishBinUseCase(NodeId(targetHandle))).thenReturn(false)
        whenever(getNodeByHandleUseCase(targetHandle)).thenReturn(targetFolder)
    }

    private fun publicFileNode(serializedData: String?) = mock<FileNode> {
        on { id } doReturn NodeId(nodeHandle)
        on { this.name } doReturn this@CheckPublicNodesNameCollisionUseCaseTest.name
        on { size } doReturn 100L
        on { modificationTime } doReturn 5L
        on { this.serializedData } doReturn serializedData
    }
}
