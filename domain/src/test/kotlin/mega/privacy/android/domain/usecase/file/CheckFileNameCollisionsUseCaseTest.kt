package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckFileNameCollisionsUseCaseTest {
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getChildNodeUseCase: GetChildNodeUseCase = mock()
    private val nodeRepository: NodeRepository = mock()

    private lateinit var underTest: CheckFileNameCollisionsUseCase

    @BeforeAll
    fun setUp() {
        underTest = CheckFileNameCollisionsUseCase(
            getNodeByHandleUseCase,
            getRootNodeUseCase,
            getChildNodeUseCase,
            nodeRepository
        )
    }

    @BeforeAll
    fun reset() {
        reset(
            getNodeByHandleUseCase,
            getRootNodeUseCase,
            getChildNodeUseCase,
            nodeRepository
        )
    }

    @Test
    fun `test that check files name collision throw exception when parent node does not exist`() =
        runTest {
            val parentNodeId = NodeId(100L)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(1L)
            whenever(getNodeByHandleUseCase(parentNodeId.longValue)).thenReturn(null)
            val entity = mock<DocumentEntity>()
            try {
                underTest(listOf(entity), parentNodeId)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(NodeDoesNotExistsException::class.java)
            }
        }

    @Test
    fun `test that check files name collision throw exception when root node does not exist`() =
        runTest {
            val parentNodeId = NodeId(1L)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(1L)
            whenever(getRootNodeUseCase()).thenReturn(null)
            val entity = mock<DocumentEntity>()
            try {
                underTest(listOf(entity), parentNodeId)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(NodeDoesNotExistsException::class.java)
            }
        }

    @Test
    fun `test that check files name collision return empty list when no collision found`() =
        runTest {
            val parentNodeId = NodeId(100L)
            val parentNode = mock<FileNode> {
                on { id } doReturn parentNodeId
            }
            val entity = mock<DocumentEntity> {
                on { name } doReturn "file"
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(1L)
            whenever(getNodeByHandleUseCase(parentNodeId.longValue)).thenReturn(parentNode)
            whenever(getChildNodeUseCase(parentNode.id, entity.name)).thenReturn(null)
            val result = underTest(listOf(entity), parentNodeId)
            assertThat(result).isEmpty()
        }

    @Test
    fun `test that check files name collision return list of collision when collision found`() =
        runTest {
            val parentNodeId = NodeId(100L)
            val parentNode = mock<FileNode> {
                on { id } doReturn parentNodeId
            }
            val entity = mock<DocumentEntity> {
                on { name } doReturn "file"
                on { uri } doReturn UriPath("path")
                on { isFolder } doReturn false
                on { size } doReturn 100L
                on { lastModified } doReturn 100L
                on { numFiles } doReturn 0
                on { numFolders } doReturn 0
            }
            val childNode = mock<FileNode> {
                on { id } doReturn NodeId(200L)
            }
            val expect = listOf(
                FileNameCollision(
                    collisionHandle = 200L,
                    name = "file",
                    isFile = true,
                    size = 100L,
                    lastModified = 100L,
                    childFileCount = 0,
                    childFolderCount = 0,
                    parentHandle = parentNodeId.longValue,
                    path = UriPath("path")
                )
            )
            whenever(nodeRepository.getInvalidHandle()).thenReturn(1L)
            whenever(getNodeByHandleUseCase(parentNodeId.longValue)).thenReturn(parentNode)
            whenever(getChildNodeUseCase(parentNode.id, entity.name)).thenReturn(childNode)
            val result = underTest(listOf(entity), parentNodeId)
            assertThat(result).isEqualTo(expect)
        }
}