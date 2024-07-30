package mega.privacy.android.domain.usecase.node


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckChatNodesNameCollisionAndCopyUseCaseTest {
    private lateinit var underTest: CheckChatNodesNameCollisionAndCopyUseCase

    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()
    private val getChildNodeUseCase: GetChildNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val nodeRepository: NodeRepository = mock()
    private val getChatFileUseCase: GetChatFileUseCase = mock()
    private val copyTypedNodesUseCase: CopyTypedNodesUseCase = mock()


    @BeforeAll
    fun setUp() {
        underTest = CheckChatNodesNameCollisionAndCopyUseCase(
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getChildNodeUseCase = getChildNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeRepository = nodeRepository,
            getChatFileUseCase = getChatFileUseCase,
            copyTypedNodesUseCase = copyTypedNodesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbishBinUseCase,
            getChildNodeUseCase,
            getChildNodeUseCase,
            getRootNodeUseCase,
            nodeRepository,
            getChatFileUseCase,
            copyTypedNodesUseCase
        )
    }

    @Test
    fun `test that all nodes are mapped as non-conflict when all parent nodes are invalid or null`() =
        runTest {
            val newNodeParent = NodeId(INVALID_NODE_HANDLE)
            val chatNode = mock<ChatDefaultFile> {
                on { id.longValue } doReturn 123L
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
            whenever(getChatFileUseCase(1, 2, 0)).thenReturn(chatNode)
            whenever(getRootNodeUseCase()).thenReturn(null)
            assertThat(underTest(1, listOf(2), newNodeParent).collisionResult).isEqualTo(
                NodeNameCollisionsResult(
                    mapOf(123L to INVALID_NODE_HANDLE),
                    emptyMap(),
                    NodeNameCollisionType.COPY
                )
            )
        }

    @Test
    fun `test that all nodes are mapped as non-conflict when all parent nodes are in rubbish bin`() =
        runTest {
            val newNodeParent = NodeId(456L)
            val chatNode = mock<ChatDefaultFile> {
                on { id.longValue } doReturn 123L
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
            whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(true)
            whenever(getNodeByHandleUseCase(any(), any())).thenReturn(mock<FileNode>())
            whenever(getChatFileUseCase(1, 2, 0)).thenReturn(chatNode)
            whenever(getRootNodeUseCase()).thenReturn(null)

            val result = underTest(1, listOf(2), newNodeParent)

            assertThat(result.collisionResult).isEqualTo(
                NodeNameCollisionsResult(
                    mapOf(123L to 456L),
                    emptyMap(),
                    NodeNameCollisionType.COPY
                )
            )
            assertThat(result.moveRequestResult).isNull()
        }

    @Test
    fun `test that result is returned correctly and non-conflict nodes are copied when one of the nodes has a name collision`() =
        runTest {
            val newNodeParent = NodeId(456L)
            val chatNode = mock<ChatDefaultFile> {
                on { id.longValue } doReturn 123L
            }
            val chatNode2 = mock<ChatDefaultFile> {
                on { id.longValue } doReturn 345L
                on { name } doReturn "chat"
                on { size }.thenReturn(Random(1000L).nextLong())
                on { modificationTime }.thenReturn(System.currentTimeMillis())
                on { creationTime }.thenReturn(System.currentTimeMillis())
            }
            val conflictNode = mock<ChatDefaultFile> {
                on { id } doReturn NodeId(789L)
            }
            val parentNode = mock<FolderNode> {
                on { id.longValue } doReturn 456L
                on { childFileCount }.thenReturn(Random(1000).nextInt())
                on { childFolderCount }.thenReturn(Random(1000).nextInt())
            }
            whenever(getChatFileUseCase(1, 2, 0)).thenReturn(chatNode)
            whenever(getChatFileUseCase(1, 3, 0)).thenReturn(chatNode2)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
            whenever(getNodeByHandleUseCase(456L, false)).thenReturn(parentNode)
            whenever(getChildNodeUseCase(NodeId(456L), chatNode2.name)).thenReturn(conflictNode)
            whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(false)
            whenever(copyTypedNodesUseCase(listOf(chatNode), newNodeParent)).thenReturn(
                MoveRequestResult.Copy(1, 1)
            )

            val result = underTest(1, listOf(2, 3), newNodeParent)

            assertThat(result.collisionResult).isEqualTo(
                NodeNameCollisionsResult(
                    noConflictNodes = mapOf(123L to 456L),
                    conflictNodes = mapOf(
                        345L to NodeNameCollision.Chat(
                            collisionHandle = conflictNode.id.longValue,
                            nodeHandle = 345L,
                            parentHandle = 456L,
                            name = chatNode2.name,
                            size = chatNode2.size,
                            childFolderCount = parentNode.childFolderCount,
                            childFileCount = parentNode.childFileCount,
                            lastModified = chatNode2.modificationTime,
                            isFile = true,
                            chatId = 1,
                            messageId = 3
                        )
                    ),
                    type = NodeNameCollisionType.COPY
                )
            )

            assertThat(result.moveRequestResult?.count).isEqualTo(1)
        }

    @Test
    fun `test that NodeDoesNotExistsException is thrown when no nodes can be found by handle`() =
        runTest {
            val chatId = 1L
            val messageIds = listOf(100L, 101L, 102L)
            val newNodeParent = NodeId(1L)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
            whenever(getChatFileUseCase(chatId, 100, 0)).thenReturn(mock<ChatDefaultFile>())
            whenever(getChatFileUseCase(chatId, 101, 0)).thenReturn(mock<ChatDefaultFile>())
            whenever(getChatFileUseCase(chatId, 102, 0)).thenReturn(mock<ChatDefaultFile>())
            whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(false)
            whenever(getNodeByHandleUseCase(1L, true)).thenReturn(null)

            try {
                underTest(chatId, messageIds, newNodeParent)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(NodeDoesNotExistsException::class.java)
            }
        }

    companion object {
        private const val INVALID_NODE_HANDLE = -1L
    }
}