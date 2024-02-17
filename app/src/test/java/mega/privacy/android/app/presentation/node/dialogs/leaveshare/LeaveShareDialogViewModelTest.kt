package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.movenode.mapper.LeaveShareRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.LeaveSharesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveShareDialogViewModelTest {

    private val leaveSharesUseCase: LeaveSharesUseCase = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val leaveShareRequestMessageMapper: LeaveShareRequestMessageMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    private val underTest = LeaveShareDialogViewModel(
        leaveSharesUseCase = leaveSharesUseCase,
        applicationScope = applicationScope,
        leaveShareRequestMessageMapper = leaveShareRequestMessageMapper,
        snackBarHandler = snackBarHandler
    )

    private val handles = listOf(1234L, 2345L)

    @Test
    fun `test that moveNodesToRubbishUseCase is invoked when calling moveNodesToRubbishBin`() =
        runTest {
            val leaveShareRequest = MoveRequestResult.DeleteMovement(2, 1, handles)

            val nodeIds = handles.map { NodeId(it) }
            whenever(leaveSharesUseCase(nodeIds)).thenReturn(leaveShareRequest)

            whenever(leaveShareRequestMessageMapper(leaveShareRequest)).thenReturn("Some value")

            underTest.onLeaveShareConfirmClicked(handles)

            verify(leaveSharesUseCase).invoke(nodeIds)
            verify(snackBarHandler).postSnackbarMessage(message = "Some value")
        }

    @AfterEach
    fun resetMocks() {
        reset(
            leaveSharesUseCase,
            leaveShareRequestMessageMapper,
            snackBarHandler
        )
    }
}