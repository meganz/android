package mega.privacy.android.domain.usecase.node


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyTypedNodesUseCaseTest {
    private lateinit var underTest: CopyTypedNodesUseCase

    private val copyTypedNodeUseCase: CopyTypedNodeUseCase = mock()
    private val newParentNode = NodeId(158401030174851)

    @BeforeAll
    fun setUp() {
        underTest = CopyTypedNodesUseCase(
            copyTypedNodeUseCase = copyTypedNodeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(copyTypedNodeUseCase)
    }


    @Test
    fun `test that ForeignNodeException is thrown when copy node throws ForeignNodeException`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenThrow(ForeignNodeException::class.java)

            assertThrows<ForeignNodeException> {
                underTest(listOf(typedNode), newParentNode)
            }
        }

    @Test
    fun `test that NotEnoughQuotaMegaException is thrown when copy node throws NotEnoughQuotaMegaException`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenAnswer {
                throw NotEnoughQuotaMegaException(1, "")
            }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(listOf(typedNode), newParentNode)
            }
        }

    @Test
    fun `test that QuotaExceededMegaException is thrown when copy node throws QuotaExceededMegaException`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenAnswer {
                throw QuotaExceededMegaException(1, "")
            }
            assertThrows<QuotaExceededMegaException> {
                underTest(listOf(typedNode), newParentNode)
            }
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when at least one node is copied successfully`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            val typedNode2 = mock<DefaultTypedFileNode>()
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(1234567890))
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode2,
                    newNodeParent = newParentNode
                )
            ).thenThrow(SecurityException::class.java)

            val result = underTest(listOf(typedNode, typedNode2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when all nodes are copied successfully`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            val typedNode2 = mock<DefaultTypedFileNode>()
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(1234567890))
            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode2,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(54567890))

            val result = underTest(listOf(typedNode, typedNode2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when copying all nodes failed`() =
        runTest {
            val typedNode = mock<DefaultTypedFileNode>()
            val typedNode2 = mock<DefaultTypedFileNode>()

            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode,
                    newNodeParent = newParentNode
                )
            ).thenThrow(RuntimeException::class.java)

            whenever(
                copyTypedNodeUseCase(
                    nodeToCopy = typedNode2,
                    newNodeParent = newParentNode
                )
            ).thenThrow(RuntimeException::class.java)

            val result = underTest(listOf(typedNode, typedNode2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(2)
        }
}