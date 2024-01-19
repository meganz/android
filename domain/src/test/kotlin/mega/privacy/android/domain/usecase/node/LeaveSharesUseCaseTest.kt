package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveSharesUseCaseTest {
    private val nodeRepository: NodeRepository = mock()

    private val underTest = LeaveSharesUseCase(nodeRepository = nodeRepository)

    @Test
    fun `test that returns correctly when delete one of the nodes failed`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(nodeRepository.leaveShareByHandle(nodes[0])).thenReturn(Unit)
        whenever(nodeRepository.leaveShareByHandle(nodes[1])).thenThrow(RuntimeException::class.java)
        val expected = MoveRequestResult.DeleteMovement(2, 1, nodes.map { it.longValue })
        val actual = underTest(nodes)
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @Test
    fun `test that returns correctly when delete all nodes failed`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(nodeRepository.leaveShareByHandle(NodeId(any()))).thenThrow(RuntimeException::class.java)
        val expected = MoveRequestResult.DeleteMovement(2, 2, nodes.map { it.longValue })
        val actual = underTest(nodes)
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @Test
    fun `test that returns correctly when delete all nodes success`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(nodeRepository.leaveShareByHandle(NodeId(any()))).thenReturn(Unit)
        val expected = MoveRequestResult.DeleteMovement(2, 0, nodes.map { it.longValue })
        val actual = underTest(nodes)
        Truth.assertThat(actual).isInstanceOf(MoveRequestResult.DeleteMovement::class.java)
        Truth.assertThat(actual.count).isEqualTo(expected.count)
        Truth.assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository)
    }
}
