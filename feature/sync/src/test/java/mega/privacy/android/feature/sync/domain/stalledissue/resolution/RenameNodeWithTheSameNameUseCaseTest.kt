package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.AddCounterToNodeNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameNodeWithTheSameNameUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class RenameNodeWithTheSameNameUseCaseTest {

    private val addCounterToNodeNameUseCase: AddCounterToNodeNameUseCase = mock()
    private val underTest: RenameNodeWithTheSameNameUseCase =
        RenameNodeWithTheSameNameUseCase(addCounterToNodeNameUseCase)

    @Test
    fun `test renaming nodes with the same name adds counters`() = runTest {
        val node1 = NodeId(123)
        val node2 = NodeId(456)
        val nodeIdsWithNames = listOf(
            node1 to "path/to/file.txt", node2 to "path/to/FILE.txt"
        )

        underTest(nodeIdsWithNames)

        verify(addCounterToNodeNameUseCase).invoke("FILE.txt", node2, 1)
    }
}
