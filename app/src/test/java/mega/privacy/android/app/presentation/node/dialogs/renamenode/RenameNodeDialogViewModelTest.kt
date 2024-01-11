package mega.privacy.android.app.presentation.node.dialogs.renamenode

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import mega.privacy.android.app.R
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidNameType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.reset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class RenameNodeDialogViewModelTest {

    private lateinit var viewModel: RenameNodeDialogViewModel
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val checkForValidNameUseCase: CheckForValidNameUseCase = mock()
    private val renameNodeUseCase: RenameNodeUseCase = mock()

    @BeforeEach
    fun setup() {
        viewModel = RenameNodeDialogViewModel(
            getNodeByHandleUseCase,
            checkForValidNameUseCase,
            renameNodeUseCase
        )
    }

    @BeforeAll
    fun setUpBeforeAll() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        reset(getNodeByHandleUseCase, checkForValidNameUseCase, renameNodeUseCase)
    }

    @AfterAll
    fun tearDownAfterAll() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that OnLoadNodeName updates state with node name`() = runTest {
        val nodeId = 123L
        val nodeName = "Test Node"
        val node: FileNode = mock()
        whenever(node.name).thenReturn(nodeName)
        whenever(getNodeByHandleUseCase(nodeId)).thenReturn(node)

        viewModel.handleAction(RenameNodeDialogAction.OnLoadNodeName(nodeId))

        assertThat(nodeName).isEqualTo(viewModel.state.value.nodeName)
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

            viewModel.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            verify(renameNodeUseCase).invoke(nodeId.longValue, newNodeName)
            assertThat(viewModel.state.value.renameValidationPassedEvent).isEqualTo(triggered)
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

            viewModel.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            assertThat(viewModel.state.value.errorMessage).isEqualTo(validationTexts.second)
        }

    @Test
    fun `test that OnRenameSucceeded updates state with success event`() = runTest {
        viewModel.handleAction(RenameNodeDialogAction.OnRenameSucceeded)

        val currentState = viewModel.state.value
        Truth.assertThat(currentState.renameSuccessfulEvent).isEqualTo(consumed)

    }

    @Test
    fun `test that OnRenameValidationPassed updates state with validation passed event`() =
        runTest {
            viewModel.handleAction(RenameNodeDialogAction.OnRenameValidationPassed)

            val currentState = viewModel.state.value
            assertThat(currentState.renameValidationPassedEvent).isEqualTo(consumed)
        }

    private fun provideValidationParameters(): List<Pair<ValidNameType, Int>> = listOf(
        ValidNameType.BLANK_NAME to R.string.invalid_string,
        ValidNameType.INVALID_NAME to R.string.invalid_characters_defined,
        ValidNameType.NAME_ALREADY_EXISTS to R.string.same_file_name_warning,
        ValidNameType.NO_EXTENSION to R.string.file_without_extension_warning,
    )
}
