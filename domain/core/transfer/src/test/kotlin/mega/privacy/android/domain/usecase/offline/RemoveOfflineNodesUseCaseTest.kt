package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveOfflineNodesUseCaseTest {
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase = mock()
    private val underTest = RemoveOfflineNodesUseCase(
        removeOfflineNodeUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            removeOfflineNodeUseCase,
        )
    }

    @Test
    fun `test that the use case returns correctly when one of the nodes failed to delete`() =
        runTest {
            val nodes = listOf(NodeId(1L), NodeId(2L))
            whenever(removeOfflineNodeUseCase(nodes[0])).thenReturn(Unit)
            whenever(removeOfflineNodeUseCase(nodes[1])).thenThrow(RuntimeException::class.java)
            val expected = MoveRequestResult.RemoveOffline(2, 1)
            val actual = underTest(nodes)
            assertThat(actual).isInstanceOf(MoveRequestResult.RemoveOffline::class.java)
            assertThat(actual.count).isEqualTo(expected.count)
            assertThat(actual.errorCount).isEqualTo(expected.errorCount)
        }

    @Test
    fun `test that the use case returns correctly when all nodes failed to delete`() = runTest {
        val nodes = listOf(NodeId(1L), NodeId(2L))
        whenever(removeOfflineNodeUseCase(NodeId(any()))).thenThrow(RuntimeException::class.java)
        val expected = MoveRequestResult.RemoveOffline(2, 2)
        val actual = underTest(nodes)
        assertThat(actual).isInstanceOf(MoveRequestResult.RemoveOffline::class.java)
        assertThat(actual.count).isEqualTo(expected.count)
        assertThat(actual.errorCount).isEqualTo(expected.errorCount)
    }

    @Test
    fun `test that the use case returns correctly when all nodes are successfully deleted`() =
        runTest {
            val nodes = listOf(NodeId(1L), NodeId(2L))
            whenever(removeOfflineNodeUseCase(NodeId(any()))).thenReturn(Unit)
            val expected = MoveRequestResult.RemoveOffline(2, 0)
            val actual = underTest(nodes)
            assertThat(actual).isInstanceOf(MoveRequestResult.RemoveOffline::class.java)
            assertThat(actual.count).isEqualTo(expected.count)
            assertThat(actual.errorCount).isEqualTo(expected.errorCount)
        }
}