package mega.privacy.android.core.nodecomponents.dialog.rename

import android.content.Context
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.core.nodecomponents.mapper.message.NodeNameErrorMessageMapper
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class RenameNodeDialogViewModelTest {

    private lateinit var underTest: RenameNodeDialogViewModel
    private val context: Context = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val checkForValidNameUseCase: CheckForValidNameUseCase = mock()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val renameNodeUseCase: RenameNodeUseCase = mock()
    private val nodeNameErrorMessageMapper: NodeNameErrorMessageMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    @BeforeEach
    fun setup() {
        underTest = RenameNodeDialogViewModel(
            context = context,
            applicationScope = applicationScope,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            checkForValidNameUseCase = checkForValidNameUseCase,
            renameNodeUseCase = renameNodeUseCase,
            nodeNameErrorMessageMapper = nodeNameErrorMessageMapper,
            snackBarHandler = snackBarHandler
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            context,
            getNodeByHandleUseCase,
            checkForValidNameUseCase,
            renameNodeUseCase,
            nodeNameErrorMessageMapper,
            snackBarHandler
        )
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
    fun `test that OnRenameConfirmed triggers success state when no validation error`() =
        runTest {
            val nodeId = NodeId(123L)
            val newNodeName = "New Node Name"
            val node: FileNode = mock()
            whenever(node.name).thenReturn(newNodeName)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(checkForValidNameUseCase(newNodeName, node))
                .thenReturn(InvalidNameType.VALID)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(nodeNameErrorMessageMapper(InvalidNameType.VALID, false))
                .thenReturn(null)
            whenever(context.getString(any())).thenReturn("")

            underTest.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            assertThat(underTest.state.value.renameValidationPassedEvent)
                .isEqualTo(triggered)
        }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideValidationParameters")
    fun `test that that validation errors are handled correctly`(validationTexts: Pair<InvalidNameType, Int>) =
        runTest {
            val nodeId = NodeId(123L)
            val newNodeName = "New Node Name"
            val node: FileNode = mock()
            whenever(node.name).thenReturn(newNodeName)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(checkForValidNameUseCase(newNodeName, node)).thenReturn(validationTexts.first)
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(node)
            whenever(nodeNameErrorMessageMapper(validationTexts.first, false))
                .thenReturn(validationTexts.second)

            underTest.handleAction(
                RenameNodeDialogAction.OnRenameConfirmed(
                    nodeId.longValue,
                    newNodeName
                )
            )

            assertThat(underTest.state.value.errorMessage).isEqualTo(validationTexts.second)
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
            ).thenReturn(InvalidNameType.DIFFERENT_EXTENSION)
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

    @Test
    fun `test that rename successful should show snackbar`() = runTest {
        val nodeId = NodeId(123L)
        val newNodeName = "New Node Name"
        val mockMessage = "Node renamed successfully"

        whenever(context.getString(any())).thenReturn(mockMessage)

        underTest.renameNode(nodeId, newNodeName)

        verify(snackBarHandler).postSnackbarMessage(mockMessage)
    }

    private fun provideValidationParameters(): List<Pair<InvalidNameType, Int>> = listOf(
        InvalidNameType.BLANK_NAME to NodesR.string.invalid_string,
        InvalidNameType.INVALID_NAME to sharedR.string.general_invalid_characters_defined,
        InvalidNameType.NAME_ALREADY_EXISTS to NodesR.string.same_file_name_warning,
        InvalidNameType.NO_EXTENSION to NodesR.string.file_without_extension_warning,
    )
}
