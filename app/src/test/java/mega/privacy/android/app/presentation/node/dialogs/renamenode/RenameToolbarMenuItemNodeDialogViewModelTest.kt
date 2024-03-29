package mega.privacy.android.app.presentation.node.dialogs.renamenode

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidNameType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class RenameToolbarMenuItemNodeDialogViewModelTest {

    private lateinit var underTest: RenameNodeDialogViewModel
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val checkForValidNameUseCase: CheckForValidNameUseCase = mock()
    private val renameNodeUseCase: RenameNodeUseCase = mock()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val snackBarHandler: SnackBarHandler = mock()

    @BeforeEach
    fun setup() {
        underTest = RenameNodeDialogViewModel(
            applicationScope,
            getNodeByHandleUseCase,
            checkForValidNameUseCase,
            renameNodeUseCase,
            snackBarHandler
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getNodeByHandleUseCase, checkForValidNameUseCase, renameNodeUseCase, snackBarHandler)
    }

    @Test
    fun `test that OnLoadNodeName updates state with node name`() = runTest {
        val nodeId = 123L
        val nodeName = "Test Node"
        val node: FileNode = mock()
        whenever(node.name).thenReturn(nodeName)
        whenever(getNodeByHandleUseCase(nodeId)).thenReturn(node)
        underTest.handleAction(RenameNodeDialogAction.OnLoadNodeName(nodeId))

        assertThat(nodeName).isEqualTo(underTest.state.value.nodeName)
    }


    @Test
    fun `test that OnRenameConfirmed renames node and triggers success state when no validation error`() =
        runTest {
            val nodeId = NodeId(123L)
            val newNodeName = "New Node Name"
            val node: FileNode = mock()
            whenever(node.name).thenReturn(newNodeName)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(checkForValidNameUseCase(newNodeName, node)).thenReturn(ValidNameType.NO_ERROR)
            whenever(renameNodeUseCase(nodeId.longValue, newNodeName)).thenReturn(Unit)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)

            underTest.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            verify(renameNodeUseCase).invoke(nodeId.longValue, newNodeName)
            verify(snackBarHandler).postSnackbarMessage(R.string.context_correctly_renamed)
            assertThat(underTest.state.value.renameValidationPassedEvent).isEqualTo(triggered)
        }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideValidationParameters")
    fun `test that that validation errors are handled correctly`(validationTexts: Pair<ValidNameType, Int>) =
        runTest {
            val nodeId = NodeId(123L)
            val newNodeName = "New Node Name"
            val node: FileNode = mock()
            whenever(node.name).thenReturn(newNodeName)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(checkForValidNameUseCase(newNodeName, node)).thenReturn(validationTexts.first)
            whenever(renameNodeUseCase(nodeId.longValue, newNodeName)).thenReturn(Unit)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)

            underTest.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            assertThat(underTest.state.value.errorMessage).isEqualTo(validationTexts.second)
        }

    @Test
    fun `test that OnRenameSucceeded updates state with success event`() = runTest {
        underTest.handleAction(RenameNodeDialogAction.OnRenameSucceeded)

        verify(snackBarHandler).postSnackbarMessage(R.string.context_correctly_renamed)
    }

    @Test
    fun `test that OnRenameValidationPassed updates state with validation passed event`() =
        runTest {
            underTest.handleAction(RenameNodeDialogAction.OnRenameValidationPassed)

            val currentState = underTest.state.value
            assertThat(currentState.renameValidationPassedEvent).isEqualTo(consumed)
        }

    @Test
    fun `test that changing extension updates showChangeNodeExtensionDialog to triggered`() =
        runTest {
            val nodeId = NodeId(123L)
            val newNodeName = "New Node Name"
            val node: FileNode = mock()
            whenever(node.name).thenReturn(newNodeName)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(
                checkForValidNameUseCase(
                    newNodeName,
                    node
                )
            ).thenReturn(ValidNameType.DIFFERENT_EXTENSION)
            whenever(renameNodeUseCase(nodeId.longValue, newNodeName)).thenReturn(Unit)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)

            underTest.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            assertThat(underTest.state.value.showChangeNodeExtensionDialogEvent).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }

    @Test
    fun `test that OnChangeNodeExtensionDialogShown updates showChangeNodeExtensionDialog to consumed`() =
        runTest {
            underTest.handleAction(RenameNodeDialogAction.OnChangeNodeExtensionDialogShown)

            assertThat(underTest.state.value.showChangeNodeExtensionDialogEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }

    private fun provideValidationParameters(): List<Pair<ValidNameType, Int>> = listOf(
        ValidNameType.BLANK_NAME to R.string.invalid_string,
        ValidNameType.INVALID_NAME to R.string.invalid_characters_defined,
        ValidNameType.NAME_ALREADY_EXISTS to R.string.same_file_name_warning,
        ValidNameType.NO_EXTENSION to R.string.file_without_extension_warning,
    )
}
