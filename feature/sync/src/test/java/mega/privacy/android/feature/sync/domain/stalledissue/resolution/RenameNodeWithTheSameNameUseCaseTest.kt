package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.AddCounterToNodeNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameNodeWithTheSameNameUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

internal class RenameNodeWithTheSameNameUseCaseTest {

    private val addCounterToNodeNameUseCase: AddCounterToNodeNameUseCase = mock()
    private val underTest: RenameNodeWithTheSameNameUseCase =
        RenameNodeWithTheSameNameUseCase(addCounterToNodeNameUseCase)

    @Test
    fun `test renaming nodes with the same name adds counters and initial node skipped`() =
        runTest {
            val node1 = NodeId(123)
            val node2 = NodeId(456)
            val nodeIdsWithNames = listOf(
                node1 to "path/to/file.txt", node2 to "path/to/FILE.txt"
            )

            underTest(nodeIdsWithNames)

            verify(addCounterToNodeNameUseCase).invoke("FILE.txt", node2, 1)
            verify(addCounterToNodeNameUseCase, times(0)).invoke("file.txt", node1, 1)
        }

    @Test
    fun `test renaming nodes with only one node adds counters`() = runTest {
        val node1 = NodeId(123)
        val nodeIdsWithNames = listOf(
            node1 to "path/to/file.txt"
        )

        underTest(nodeIdsWithNames)

        verify(addCounterToNodeNameUseCase).invoke("file.txt", node1, 1)
    }


}
