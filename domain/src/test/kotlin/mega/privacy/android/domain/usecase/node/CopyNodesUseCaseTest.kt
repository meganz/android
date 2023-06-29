package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyNodesUseCaseTest {
    private lateinit var underTest: CopyNodesUseCase

    private val copyNodeUseCase: CopyNodeUseCase = mock()
    private val accountRepository: AccountRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = CopyNodesUseCase(
            copyNodeUseCase = copyNodeUseCase, accountRepository = accountRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(copyNodeUseCase, accountRepository)
    }


    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(copyNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
                .thenThrow(ForeignNodeException::class.java)
            assertThrows<ForeignNodeException> {
                underTest(mapOf(1L to 2L))
            }
        }

    @Test
    fun `test that throw NotEnoughQuotaMegaException when move node throw NotEnoughQuotaMegaException`() =
        runTest {
            whenever(copyNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
                .thenAnswer {
                    throw NotEnoughQuotaMegaException(1, "")
                }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(mapOf(1L to 2L))
            }
        }

    @Test
    fun `test that throw QuotaExceededMegaException when move node throw QuotaExceededMegaException`() =
        runTest {
            whenever(copyNodeUseCase(NodeId(any()), NodeId(any()), anyOrNull()))
                .thenAnswer {
                    throw QuotaExceededMegaException(1, "")
                }
            assertThrows<QuotaExceededMegaException> {
                underTest(mapOf(1L to 2L))
            }
        }

    @Test
    fun `test that return MoveRequestResult correctly when at least one move successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val moveSecondNode = 2L to 101L
            whenever(
                copyNodeUseCase(
                    NodeId(moveFirstNode.first),
                    NodeId(moveFirstNode.second),
                    null
                )
            )
                .thenReturn(NodeId(moveFirstNode.first))
            whenever(
                copyNodeUseCase(
                    NodeId(moveSecondNode.first),
                    NodeId(moveSecondNode.second),
                    null
                )
            )
                .thenThrow(SecurityException::class.java)
            whenever(accountRepository.resetAccountDetailsTimeStamp()).thenReturn(Unit)
            val result = underTest(mapOf(moveFirstNode, moveSecondNode))
            verify(accountRepository).resetAccountDetailsTimeStamp()
            Truth.assertThat(result.count).isEqualTo(2)
            Truth.assertThat(result.successCount).isEqualTo(1)
            Truth.assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that return MoveRequestResult correctly when move all nodes successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val moveSecondNode = 2L to 101L
            whenever(
                copyNodeUseCase(
                    NodeId(moveFirstNode.first),
                    NodeId(moveFirstNode.second),
                    null
                )
            )
                .thenReturn(NodeId(moveFirstNode.first))
            whenever(
                copyNodeUseCase(
                    NodeId(moveSecondNode.first),
                    NodeId(moveSecondNode.second),
                    null
                )
            )
                .thenReturn(NodeId(moveSecondNode.first))
            whenever(accountRepository.resetAccountDetailsTimeStamp()).thenReturn(Unit)
            val result = underTest(mapOf(moveFirstNode, moveSecondNode))
            verify(accountRepository).resetAccountDetailsTimeStamp()
            Truth.assertThat(result.count).isEqualTo(2)
            Truth.assertThat(result.successCount).isEqualTo(2)
            Truth.assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that return MoveRequestResult correctly when move all nodes failed`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val moveSecondNode = 2L to 101L
            whenever(
                copyNodeUseCase(
                    NodeId(moveFirstNode.first),
                    NodeId(moveFirstNode.second),
                    null
                )
            )
                .thenThrow(RuntimeException::class.java)
            whenever(
                copyNodeUseCase(
                    NodeId(moveSecondNode.first),
                    NodeId(moveSecondNode.second),
                    null
                )
            )
                .thenThrow(RuntimeException::class.java)
            whenever(accountRepository.resetAccountDetailsTimeStamp()).thenReturn(Unit)
            val result = underTest(mapOf(moveFirstNode, moveSecondNode))
            verify(accountRepository, times(0)).resetAccountDetailsTimeStamp()
            Truth.assertThat(result.count).isEqualTo(2)
            Truth.assertThat(result.successCount).isEqualTo(0)
            Truth.assertThat(result.errorCount).isEqualTo(2)
        }
}