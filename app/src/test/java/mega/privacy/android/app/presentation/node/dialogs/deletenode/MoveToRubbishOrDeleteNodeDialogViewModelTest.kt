package mega.privacy.android.app.presentation.node.dialogs.deletenode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class MoveToRubbishOrDeleteNodeDialogViewModelTest {
    private val deleteNodesUseCase: DeleteNodesUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val moveRequestMessageMapper: MoveRequestMessageMapper = mock()

    private val handles = listOf(
        1L,
        2L
    )
    private val underTest = MoveToRubbishOrDeleteNodeDialogViewModel(
        deleteNodesUseCase = deleteNodesUseCase,
        moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
        moveRequestMessageMapper = moveRequestMessageMapper
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that move nodes to rubbish is called, it calls moveNodesToRubbishUseCase`() =
        runTest {

            underTest.moveNodesToRubbishBin(handles)
            verify(moveNodesToRubbishUseCase).invoke(handles)
        }

    @Test
    fun `test that delete node is called, it calls deleteNodesUseCase`() =
        runTest {
            val nodeHandles = handles.map {
                NodeId(it)
            }
            underTest.deleteNodes(handles)
            verify(deleteNodesUseCase).invoke(nodeHandles)
        }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}