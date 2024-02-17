package mega.privacy.android.app.presentation.node.dialogs.changeextension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
internal class ChangeNodeExtensionDialogViewModelTest {

    private lateinit var underTest: ChangeNodeExtensionDialogViewModel
    private val renameNodeUseCase = mock<RenameNodeUseCase>()
    private val snackBarHandler: SnackBarHandler = mock()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeEach
    fun setup() {
        underTest = ChangeNodeExtensionDialogViewModel(
            applicationScope = applicationScope,
            renameNodeUseCase = renameNodeUseCase,
            snackBarHandler = snackBarHandler
        )
    }

    @AfterEach
    fun tearDown() {
        reset(renameNodeUseCase)
    }

    @Test
    fun `test that OnChangeExtensionConfirmed calls renameNodeUseCase and updates state`() =
        runTest {
            val nodeId = 123L
            val newNodeName = "New Node Name"
            whenever(renameNodeUseCase(nodeId, newNodeName)).thenReturn(Unit)
            underTest.handleAction(
                ChangeNodeExtensionAction.OnChangeExtensionConfirmed(
                    nodeId, newNodeName
                )
            )

            verify(renameNodeUseCase).invoke(nodeId, newNodeName)
            verify(snackBarHandler).postSnackbarMessage(R.string.context_correctly_renamed)
        }
}