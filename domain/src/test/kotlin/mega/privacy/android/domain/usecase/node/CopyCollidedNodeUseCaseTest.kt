package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.chat.GetChatFilesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

internal class CopyCollidedNodeUseCaseTest {

    private val getChatFilesUseCase: GetChatFilesUseCase = mock()
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase = mock()
    private val nodeRepository: NodeRepository = mock()

    private lateinit var underTest: CopyCollidedNodeUseCase

    @BeforeEach
    fun setUp() {
        underTest = CopyCollidedNodeUseCase(
            getChatFilesUseCase = getChatFilesUseCase,
            copyTypedNodeUseCase = copyTypedNodeUseCase,
            nodeRepository = nodeRepository
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository, copyTypedNodeUseCase, getChatFilesUseCase)
    }

    @Test
    fun `test that CopyCollidedNodeUseCase correctly copies a node when there is no exception`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { renameName } doReturn "new name"
                on { serializedData } doReturn null
            }
            whenever(
                nodeRepository.copyNode(
                    NodeId(1L),
                    null,
                    NodeId(2L),
                    "new name"
                )
            ).thenReturn(
                NodeId(
                    1L
                )
            )

            val result = underTest(nodeNameCollision, rename = true)

            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that CopyCollidedNodeUseCase correctly handles exceptions and returns the appropriate MoveRequestResult`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { renameName } doReturn "new name"
                on { serializedData } doReturn null
            }
            whenever(
                nodeRepository.copyNode(
                    NodeId(any()),
                    anyOrNull(),
                    NodeId(any()),
                    anyOrNull()
                )
            ).thenThrow(RuntimeException::class.java)

            val result = underTest(nodeNameCollision, rename = true)

            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that CopyCollidedNodeUseCase correctly copies a chat node when there is no exception`() =
        runTest {
            val chatFile = mock<ChatDefaultFile> {
                on { id } doReturn NodeId(1L)
            }
            val nodeNameCollision = mock<NodeNameCollision.Chat> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { chatId } doReturn 1L
                on { messageId } doReturn 1L
                on { serializedData } doReturn null
            }
            whenever(getChatFilesUseCase(1L, 1L)).thenReturn(listOf(chatFile))
            whenever(copyTypedNodeUseCase(chatFile, NodeId(2L), null)).thenReturn(NodeId(2L))

            val result = underTest(nodeNameCollision, rename = false)

            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
        }


    @Test
    fun `test that CopyCollidedNodeUseCase correctly handles exceptions and returns the appropriate MoveRequestResult when chat file is not found`() =
        runTest {
            val chatFile = mock<ChatDefaultFile> {
                on { id } doReturn NodeId(9L)
            }
            val name = "new name"
            val nodeNameCollision = mock<NodeNameCollision.Chat> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { renameName } doReturn "new name"
                on { chatId } doReturn 1L
                on { messageId } doReturn 1L
            }
            whenever(getChatFilesUseCase(1L, 1L)).thenReturn(listOf(chatFile))
            whenever(copyTypedNodeUseCase(chatFile, NodeId(2L), name)).thenReturn(NodeId(1L))

            val result = underTest(nodeNameCollision, rename = true)

            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that CopyCollidedNodeUseCase correctly handles exceptions and returns the appropriate MoveRequestResult when copying a chat node`() =
        runTest {
            val chatFile = mock<ChatDefaultFile>()
            val nodeNameCollision = mock<NodeNameCollision.Chat> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { renameName } doReturn "new name"
                on { chatId } doReturn 1L
                on { messageId } doReturn 1L
            }
            whenever(getChatFilesUseCase(any(), any())).thenThrow(RuntimeException::class.java)
            whenever(
                copyTypedNodeUseCase(
                    chatFile,
                    NodeId(2L),
                    null
                )
            ).thenThrow(RuntimeException::class.java)

            val result = underTest(nodeNameCollision, rename = false)

            assert(result.count == 1)
            assert(result.errorCount == 1)
        }

    @Test
    fun `test that CopyCollidedNodeUseCase throws exception when copy node throw ForeignNodeException`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { serializedData } doReturn null
            }
            whenever(
                nodeRepository.copyNode(
                    NodeId(any()),
                    anyOrNull(),
                    NodeId(any()),
                    anyOrNull()
                )
            )
                .thenThrow(ForeignNodeException::class.java)

            assertThrows<ForeignNodeException> {
                underTest(nodeNameCollision, false)
            }
        }

    @Test
    fun `test that CopyCollidedNodeUseCase throws exception when copy node throw NotEnoughQuotaMegaException`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { serializedData } doReturn "data"
            }
            whenever(
                nodeRepository.copyNode(
                    NodeId(any()),
                    anyOrNull(),
                    NodeId(any()),
                    anyOrNull()
                )
            )
                .thenAnswer {
                    throw NotEnoughQuotaMegaException(1, "")
                }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(nodeNameCollision, false)
            }
        }

    @Test
    fun `test that CopyCollidedNodeUseCase throws exception when copy node throw QuotaExceededMegaException`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { serializedData } doReturn "data"
            }
            whenever(
                nodeRepository.copyNode(
                    NodeId(any()),
                    anyOrNull(),
                    NodeId(any()),
                    anyOrNull()
                )
            )
                .thenAnswer {
                    throw QuotaExceededMegaException(1, "")
                }

            assertThrows<QuotaExceededMegaException> {
                underTest(nodeNameCollision, false)
            }
        }

}