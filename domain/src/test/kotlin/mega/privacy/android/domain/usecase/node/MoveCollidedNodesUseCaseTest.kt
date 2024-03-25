package mega.privacy.android.domain.usecase.node


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveCollidedNodesUseCaseTest {
    private lateinit var underTest: MoveCollidedNodesUseCase

    private val moveCollidedNodeUseCase: MoveCollidedNodeUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = MoveCollidedNodesUseCase(
            moveCollidedNodeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(moveCollidedNodeUseCase)
    }

    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(moveCollidedNodeUseCase(any(), any()))
                .thenThrow(ForeignNodeException::class.java)
            assertThrows<ForeignNodeException> {
                underTest(
                    listOf(mock<NodeNameCollision> {
                        on { renameName } doReturn "new name"
                    }),
                    rename = true
                )
            }
        }

    @Test
    fun `test that throw NotEnoughQuotaMegaException when move node throw NotEnoughQuotaMegaException`() =
        runTest {
            whenever(moveCollidedNodeUseCase(any(), any()))
                .thenAnswer {
                    throw NotEnoughQuotaMegaException(1, "")
                }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(
                    listOf(mock<NodeNameCollision> {
                        on { renameName } doReturn "new name"
                    }),
                    rename = true
                )
            }
        }

    @Test
    fun `test that throw QuotaExceededMegaException when move node throw QuotaExceededMegaException`() =
        runTest {
            whenever(moveCollidedNodeUseCase(any(), any()))
                .thenAnswer {
                    throw QuotaExceededMegaException(1, "")
                }
            assertThrows<QuotaExceededMegaException> {
                underTest(
                    listOf(mock<NodeNameCollision> {
                        on { renameName } doReturn "new name"
                    }),
                    rename = true
                )
            }
        }

    @Test
    fun `test that return MoveRequestResult correctly when at least one node moved successfully`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision>()
            val nodeNameCollision2 = mock<NodeNameCollision>()
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision,
                    rename = true
                )
            ).thenReturn(MoveRequestResult.GeneralMovement(1, 0))
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision2,
                    rename = true
                )
            ).thenThrow(RuntimeException::class.java)

            val result = underTest(
                listOf(nodeNameCollision, nodeNameCollision2),
                rename = true
            )
            print(result.toString())
            assertThat(result.count).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that return MoveRequestResult correctly when at all nodes moved successfully`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision>()
            val nodeNameCollision2 = mock<NodeNameCollision>()
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision,
                    rename = true
                )
            ).thenReturn(MoveRequestResult.GeneralMovement(1, 0))
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision2,
                    rename = true
                )
            ).thenReturn(MoveRequestResult.GeneralMovement(1, 0))

            val result = underTest(
                listOf(nodeNameCollision, nodeNameCollision2),
                rename = true
            )
            print(result.toString())
            assertThat(result.count).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that return MoveRequestResult correctly when moving all nodes failed`() =
        runTest {
            val nodeNameCollision = mock<NodeNameCollision>()
            val nodeNameCollision2 = mock<NodeNameCollision>()
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision,
                    rename = true
                )
            ).thenThrow(RuntimeException::class.java)
            whenever(
                moveCollidedNodeUseCase(
                    nodeNameCollision2,
                    rename = true
                )
            ).thenThrow(RuntimeException::class.java)

            val result = underTest(
                listOf(nodeNameCollision, nodeNameCollision2),
                rename = true
            )
            print(result.toString())
            assertThat(result.count).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(2)
        }
}