package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyCollidedNodesUseCaseTest {
    private lateinit var underTest: CopyCollidedNodesUseCase

    private val copyCollidedNodeUseCase: CopyCollidedNodeUseCase = mock()
    private val accountRepository: AccountRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = CopyCollidedNodesUseCase(
            copyCollidedNodeUseCase = copyCollidedNodeUseCase,
            accountRepository = accountRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(copyCollidedNodeUseCase, accountRepository)
    }


    @Test
    fun `test that ForeignNodeException is thrown when move node throw ForeignNodeException`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = true
                )
            ).thenThrow(ForeignNodeException::class.java)

            assertThrows<ForeignNodeException> {
                underTest(listOf(nameCollision), true)
            }
        }

    @Test
    fun `test that NotEnoughQuotaMegaException is thrown when move node throw NotEnoughQuotaMegaException`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = true
                )
            ).thenAnswer {
                throw NotEnoughQuotaMegaException(1, "")
            }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(listOf(nameCollision), true)
            }
        }

    @Test
    fun `test that QuotaExceededMegaException thrown when move node throw QuotaExceededMegaException`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = true
                )
            ).thenAnswer {
                throw QuotaExceededMegaException(1, "")
            }
            assertThrows<QuotaExceededMegaException> {
                underTest(listOf(nameCollision), true)
            }
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when at least one node is successfully moved`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            val nameCollision2 = mock<NodeNameCollision.Default>()

            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = false
                )
            ).thenReturn(MoveRequestResult.Copy(1, 0))
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision2,
                    rename = false
                )
            ).thenThrow(SecurityException::class.java)

            val result = underTest(listOf(nameCollision, nameCollision2), false)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
            verify(accountRepository).resetAccountDetailsTimeStamp()
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when all nodes are successfully moved`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            val nameCollision2 = mock<NodeNameCollision.Default>()

            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = true
                )
            ).thenReturn(MoveRequestResult.Copy(1, 0))
            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision2,
                    rename = true
                )
            ).thenReturn(MoveRequestResult.Copy(1, 0))

            val result = underTest(listOf(nameCollision, nameCollision2), true)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(0)
            verify(accountRepository).resetAccountDetailsTimeStamp()
        }

    @Test
    fun `test that MoveRequestResult is returned correctly when all nodes failed to move`() =
        runTest {
            val nameCollision = mock<NodeNameCollision.Default>()
            val nameCollision2 = mock<NodeNameCollision.Default>()

            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = true
                )
            ).thenThrow(RuntimeException::class.java)

            whenever(
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision2,
                    rename = true
                )
            ).thenThrow(RuntimeException::class.java)


            val result = underTest(listOf(nameCollision, nameCollision2), true)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(2)
            verify(accountRepository, never()).resetAccountDetailsTimeStamp()
        }
}