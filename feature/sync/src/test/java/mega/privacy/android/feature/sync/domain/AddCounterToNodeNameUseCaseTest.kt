package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.usecase.AddCounterToNodeNameUseCase
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
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
