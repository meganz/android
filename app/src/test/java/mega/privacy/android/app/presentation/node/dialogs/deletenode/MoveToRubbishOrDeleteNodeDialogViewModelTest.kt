package mega.privacy.android.app.presentation.node.dialogs.deletenode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class MoveToRubbishOrDeleteNodeDialogViewModelTest {
    private val deleteNodesUseCase: DeleteNodesUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val moveRequestMessageMapper: MoveRequestMessageMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private val handles = listOf(
        1L,
        2L
    )
    private val underTest = MoveToRubbishOrDeleteNodeDialogViewModel(
        applicationScope = applicationScope,
        deleteNodesUseCase = deleteNodesUseCase,
        moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
        moveRequestMessageMapper = moveRequestMessageMapper,
        snackBarHandler = snackBarHandler
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that moveNodesToRubbishUseCase is invoked when calling moveNodesToRubbishBin`() =
        runTest {
            val moveToRubbishNode = MoveRequestResult.RubbishMovement(2, 1, handles[0])

            whenever(moveNodesToRubbishUseCase(handles)).thenReturn(moveToRubbishNode)
            whenever(moveRequestMessageMapper(moveToRubbishNode)).thenReturn("Some value")

            underTest.moveNodesToRubbishBin(handles)

            verify(moveNodesToRubbishUseCase).invoke(handles)
            verify(snackBarHandler).postSnackbarMessage("Some value")
        }

    @Test
    fun `test that deleteNodesUseCase is invoked when calling deleteNodes`() =
        runTest {
            val nodeHandles = handles.map {
                NodeId(it)
            }
            val deleteNode = MoveRequestResult.DeleteMovement(2, 1, handles)

            whenever(deleteNodesUseCase(nodeHandles)).thenReturn(deleteNode)
            whenever(moveRequestMessageMapper(deleteNode)).thenReturn("Some value")

            underTest.deleteNodes(handles)

            verify(deleteNodesUseCase).invoke(nodeHandles)
            verify(snackBarHandler).postSnackbarMessage("Some value")
        }

    @AfterEach
    fun resetMock() {
        reset(
            deleteNodesUseCase,
            moveNodesToRubbishUseCase,
            moveRequestMessageMapper,
            snackBarHandler,
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}