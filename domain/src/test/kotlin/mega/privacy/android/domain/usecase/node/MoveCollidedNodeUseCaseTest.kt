package mega.privacy.android.domain.usecase.node


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveCollidedNodeUseCaseTest {
    private lateinit var underTest: MoveCollidedNodeUseCase

    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = MoveCollidedNodeUseCase(
            moveNodeUseCase = moveNodeUseCase,
            moveNodeToRubbishBinUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(moveNodeUseCase, moveNodeToRubbishBinUseCase)
    }

    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), any()))
                .thenThrow(ForeignNodeException::class.java)
            assertThrows<ForeignNodeException> {
                underTest(
                    nodeNameCollision = mock<NodeNameCollision.Default> {
                        on { renameName } doReturn "new name"
                    },
                    rename = true
                )
            }
        }

    @Test
    fun `test that throw NotEnoughQuotaMegaException when move node throw NotEnoughQuotaMegaException`() =
        runTest {
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), any()))
                .thenAnswer {
                    throw NotEnoughQuotaMegaException(1, "")
                }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(
                    nodeNameCollision = mock<NodeNameCollision.Default> {
                        on { renameName } doReturn "new name"
                    },
                    rename = true
                )
            }
        }

    @Test
    fun `test that throw QuotaExceededMegaException when move node throw QuotaExceededMegaException`() =
        runTest {
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), any()))
                .thenAnswer {
                    throw QuotaExceededMegaException(1, "")
                }
            assertThrows<QuotaExceededMegaException> {
                underTest(
                    nodeNameCollision = mock<NodeNameCollision.Default> {
                        on { renameName } doReturn "new name"
                    },
                    rename = true
                )
            }
        }

    @Test
    fun `test that return MoveRequestResult correctly when moved successfully`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { renameName } doReturn "new name"
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
            }
            whenever(
                moveNodeUseCase(
                    NodeId(any()),
                    NodeId(any()),
                    any()
                )
            ).thenReturn(NodeId(1L))
            val result = underTest(
                nodeNameCollision,
                rename = true
            )
            print(result.toString())
            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that node is moved to rubbish bin when rename is false and node is a file`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
                on { isFile } doReturn true
                on { collisionHandle } doReturn 3L
            }
            whenever(moveNodeToRubbishBinUseCase(NodeId(any()))).thenReturn(Unit)
            whenever(
                moveNodeUseCase(
                    NodeId(any()),
                    NodeId(any()),
                    eq(null)
                )
            ).thenReturn(NodeId(1L))
            val result = underTest(
                nodeNameCollision,
                rename = false
            )
            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
            verify(moveNodeToRubbishBinUseCase).invoke(NodeId(3L))
        }


    @Test
    fun `test that return MoveRequestResult correctly when node move failed`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision.Default> {
                on { renameName } doReturn "new name"
                on { nodeHandle } doReturn 1L
                on { parentHandle } doReturn 2L
            }
            whenever(
                moveNodeUseCase(
                    NodeId(any()),
                    NodeId(any()),
                    any()
                )
            ).thenThrow(RuntimeException::class.java)
            val result = underTest(
                nodeNameCollision,
                rename = true
            )
            print(result.toString())
            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
        }
}