package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeleteNodesUseCaseTest {
    private val deleteNodeByHandleUseCase: DeleteNodeByHandleUseCase = mock()
    private val accountRepository: AccountRepository = mock()
    private val underTest = DeleteNodesUseCase(
        deleteNodeByHandleUseCase,
        accountRepository,
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            deleteNodeByHandleUseCase,
            accountRepository,
        )
    }

    @Test
    fun `test that returns correctly when delete one of the nodes failed`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(deleteNodeByHandleUseCase(nodes[0])).thenReturn(Unit)
        whenever(deleteNodeByHandleUseCase(nodes[1])).thenThrow(RuntimeException::class.java)
        val expected = MoveRequestResult.DeleteMovement(2, 1, nodes.map { it.longValue })
        val actual = underTest(nodes)
        verify(accountRepository).resetAccountDetailsTimeStamp()
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @Test
    fun `test that returns correctly when delete all nodes failed`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(deleteNodeByHandleUseCase(NodeId(any()))).thenThrow(RuntimeException::class.java)
        val expected = MoveRequestResult.DeleteMovement(2, 2, nodes.map { it.longValue })
        val actual = underTest(nodes)
        verifyNoInteractions(accountRepository)
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @Test
    fun `test that returns correctly when delete all nodes success`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(deleteNodeByHandleUseCase(NodeId(any()))).thenReturn(Unit)
        val expected = MoveRequestResult.DeleteMovement(2, 0, nodes.map { it.longValue })
        val actual = underTest(nodes)
        verify(accountRepository).resetAccountDetailsTimeStamp()
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }
}