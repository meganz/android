package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.AddCounterToNodeNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCounterToNodeNameUseCaseTest {

    private val renameNodeUseCase: RenameNodeUseCase = mock()

    private val underTest: AddCounterToNodeNameUseCase =
        AddCounterToNodeNameUseCase(renameNodeUseCase)

    @AfterEach
    fun resetAndTearDown() {
        reset(
            renameNodeUseCase,
        )
    }

    @Test
    fun `test that adding counter invokes renameNodeUseCase correctly`() = runTest {
        val nodeName = "example.txt"
        val nodeId = NodeId(123)
        val counter = 2
        val expectedRenamedNodeName = "example (2).txt"

        whenever(renameNodeUseCase.invoke(nodeId.longValue, expectedRenamedNodeName)).thenReturn(
            Unit
        )

        underTest(nodeName, nodeId, counter)

        verify(renameNodeUseCase).invoke(nodeId.longValue, expectedRenamedNodeName)
    }
}
