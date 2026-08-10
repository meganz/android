package mega.privacy.android.shared.nodes.dialog.newfile

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.file.IsValidTextFileUseCase
import mega.privacy.android.domain.usecase.node.ValidateNodeNameUseCase
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_LINK_FILE_EXTENSION
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_TEXT_FILE_EXTENSION
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewTextFileNodeDialogViewModelTest {

    private val validateNodeNameUseCase = mock<ValidateNodeNameUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val isValidTextFileUseCase = mock<IsValidTextFileUseCase>()

    private fun newViewModel(
        parentNodeId: NodeId = NodeId(123L),
        defaultExtension: String = DEFAULT_TEXT_FILE_EXTENSION,
    ) = NewTextFileNodeDialogViewModel(
        validateNodeNameUseCase = validateNodeNameUseCase,
        getRootNodeUseCase = getRootNodeUseCase,
        isValidTextFileUseCase = isValidTextFileUseCase,
        args = NewTextFileNodeDialogViewModel.Args(
            parentNodeId = parentNodeId,
            defaultExtension = defaultExtension,
        ),
    )

    @AfterEach
    fun resetMocks() {
        reset(
            validateNodeNameUseCase,
            getRootNodeUseCase,
            isValidTextFileUseCase,
        )
    }

    @Test
    fun `test that initial state has default file name and no exception`() = runTest {
        val underTest = newViewModel()

        underTest.uiState.test {
            val state = awaitData()
            assertThat(state.fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)
            assertThat(state.fileNameException).isNull()
            assertThat(state.validationSuccessEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that onFileNameChanged updates file name and clears previous exception`() = runTest {
        val parentNodeId = NodeId(123L)
        val underTest = newViewModel(parentNodeId)
        whenever(validateNodeNameUseCase(eq(""), eq(parentNodeId)))
            .thenThrow(EmptyNodeNameException())

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged("")
            assertThat(awaitData().fileName).isEmpty()

            underTest.validateFileName()
            assertThat(awaitData().fileNameException)
                .isInstanceOf(EmptyNodeNameException::class.java)

            underTest.onFileNameChanged("hello$DEFAULT_TEXT_FILE_EXTENSION")
            val state = awaitStateMatching { it.fileNameException == null }
            assertThat(state.fileName).isEqualTo("hello$DEFAULT_TEXT_FILE_EXTENSION")
        }
    }

    private suspend fun ReceiveTurbine<NewTextFileNodeDialogUiState>.awaitData(): NewTextFileNodeDialogUiState.Data {
        var item = awaitItem()
        while (item !is NewTextFileNodeDialogUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<NewTextFileNodeDialogUiState>.awaitStateMatching(
        predicate: (NewTextFileNodeDialogUiState.Data) -> Boolean,
    ): NewTextFileNodeDialogUiState.Data {
        var item = awaitData()
        while (!predicate(item)) {
            item = awaitData()
        }
        return item
    }

    @Test
    fun `test that validateFileName sets EmptyNodeNameException when name is empty`() = runTest {
        val parentNodeId = NodeId(123L)
        val underTest = newViewModel(parentNodeId)
        whenever(validateNodeNameUseCase(eq(""), eq(parentNodeId)))
            .thenThrow(EmptyNodeNameException())

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged("")
            assertThat(awaitData().fileName).isEmpty()

            underTest.validateFileName()
            val state = awaitData()
            assertThat(state.fileNameException)
                .isInstanceOf(EmptyNodeNameException::class.java)
            assertThat(state.validationSuccessEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that validateFileName trims whitespace before validating`() = runTest {
        val parentNodeId = NodeId(123L)
        val underTest = newViewModel(parentNodeId)
        whenever(validateNodeNameUseCase(eq(""), eq(parentNodeId)))
            .thenThrow(EmptyNodeNameException())

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged("   ")
            assertThat(awaitData().fileName).isEqualTo("   ")

            underTest.validateFileName()
            awaitData()
        }
        verify(validateNodeNameUseCase).invoke(eq(""), eq(parentNodeId))
    }

    @Test
    fun `test that validateFileName sets InvalidNodeNameException for invalid characters`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val underTest = newViewModel(parentNodeId)
            val fileName = "test*folder"
            whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId)))
                .thenThrow(InvalidNodeNameException())

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                assertThat(awaitData().fileNameException)
                    .isInstanceOf(InvalidNodeNameException::class.java)
            }
        }

    @Test
    fun `test that validateFileName sets NodeNameAlreadyExistsException for existing names`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val underTest = newViewModel(parentNodeId)
            val fileName = "existingFile$DEFAULT_TEXT_FILE_EXTENSION"
            whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId)))
                .thenThrow(NodeNameAlreadyExistsException())

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                assertThat(awaitData().fileNameException)
                    .isInstanceOf(NodeNameAlreadyExistsException::class.java)
            }
        }

    @Test
    fun `test that validateFileName sets InvalidNodeExtensionException for invalid extension`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val underTest = newViewModel(parentNodeId)
            val fileName = "newFile.exe"
            whenever(isValidTextFileUseCase(eq(fileName)))
                .thenThrow(InvalidNodeExtensionException())

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                assertThat(awaitData().fileNameException)
                    .isInstanceOf(InvalidNodeExtensionException::class.java)
            }
        }

    @Test
    fun `test that validateFileName triggers validation success event when valid`() = runTest {
        val parentNodeId = NodeId(123L)
        val underTest = newViewModel(parentNodeId)
        val fileName = "newFile$DEFAULT_TEXT_FILE_EXTENSION"
        whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId))).thenReturn(Unit)

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged(fileName)
            assertThat(awaitData().fileName).isEqualTo(fileName)

            underTest.validateFileName()
            val state = awaitData()
            assertThat(state.fileNameException).isNull()
            val triggered = state.validationSuccessEvent
                    as StateEventWithContentTriggered<String>
            assertThat(triggered.content).isEqualTo(fileName)
        }
        verify(validateNodeNameUseCase).invoke(eq(fileName), eq(parentNodeId))
        verify(isValidTextFileUseCase).invoke(eq(fileName))
    }

    @Test
    fun `test that validateFileName uses root node when parentNodeId is -1`() = runTest {
        val parentNodeId = NodeId(-1L)
        val underTest = newViewModel(parentNodeId)
        val fileName = "newFile$DEFAULT_TEXT_FILE_EXTENSION"
        val rootNodeId = NodeId(789L)
        val rootNode = mock<TypedNode>()

        whenever(rootNode.id).thenReturn(rootNodeId)
        whenever(getRootNodeUseCase()).thenReturn(rootNode)
        whenever(validateNodeNameUseCase(eq(fileName), eq(rootNodeId))).thenReturn(Unit)

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged(fileName)
            assertThat(awaitData().fileName).isEqualTo(fileName)

            underTest.validateFileName()
            val state = awaitData()
            val triggered = state.validationSuccessEvent
                    as StateEventWithContentTriggered<String>
            assertThat(triggered.content).isEqualTo(fileName)
        }
        verify(getRootNodeUseCase).invoke()
        verify(validateNodeNameUseCase).invoke(eq(fileName), eq(rootNodeId))
    }

    @Test
    fun `test that initial state contains parent node id from args`() = runTest {
        val parentNodeId = NodeId(456L)
        val underTest = newViewModel(parentNodeId)

        underTest.uiState.test {
            assertThat(awaitData().parentNodeId).isEqualTo(parentNodeId)
        }
    }

    @Test
    fun `test that validateFileName does not trigger success or set exception when root node is null`() =
        runTest {
            val parentNodeId = NodeId(-1L)
            val underTest = newViewModel(parentNodeId)
            val fileName = "newFile$DEFAULT_TEXT_FILE_EXTENSION"
            whenever(getRootNodeUseCase()).thenReturn(null)

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                expectNoEvents()
            }
            verify(getRootNodeUseCase).invoke()
        }

    @Test
    fun `test that initial file name uses the configured default extension`() = runTest {
        val underTest = newViewModel(defaultExtension = DEFAULT_LINK_FILE_EXTENSION)

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_LINK_FILE_EXTENSION)
        }
    }

    @Test
    fun `test that validateFileName skips isValidTextFileUseCase when name ends with the link extension`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val underTest = newViewModel(
                parentNodeId = parentNodeId,
                defaultExtension = DEFAULT_LINK_FILE_EXTENSION,
            )
            val fileName = "myShortcut$DEFAULT_LINK_FILE_EXTENSION"
            whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId))).thenReturn(Unit)

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_LINK_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                val state = awaitData()
                val triggered = state.validationSuccessEvent
                        as StateEventWithContentTriggered<String>
                assertThat(triggered.content).isEqualTo(fileName)
            }
            verify(validateNodeNameUseCase).invoke(eq(fileName), eq(parentNodeId))
            verifyNoInteractions(isValidTextFileUseCase)
        }

    @Test
    fun `test that validateFileName skips isValidTextFileUseCase and succeeds when name has no extension`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val underTest = newViewModel(parentNodeId)
            val fileName = "myFile"
            whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId))).thenReturn(Unit)

            underTest.uiState.test {
                assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

                underTest.onFileNameChanged(fileName)
                assertThat(awaitData().fileName).isEqualTo(fileName)

                underTest.validateFileName()
                val state = awaitData()
                assertThat(state.fileNameException).isNull()
                val triggered = state.validationSuccessEvent
                        as StateEventWithContentTriggered<String>
                assertThat(triggered.content).isEqualTo(fileName)
            }
            verify(validateNodeNameUseCase).invoke(eq(fileName), eq(parentNodeId))
            verifyNoInteractions(isValidTextFileUseCase)
        }

    @Test
    fun `test that onValidationSuccessEventConsumed clears the success event`() = runTest {
        val parentNodeId = NodeId(123L)
        val underTest = newViewModel(parentNodeId)
        val fileName = "newFile$DEFAULT_TEXT_FILE_EXTENSION"
        whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId))).thenReturn(Unit)

        underTest.uiState.test {
            assertThat(awaitData().fileName).isEqualTo(DEFAULT_TEXT_FILE_EXTENSION)

            underTest.onFileNameChanged(fileName)
            assertThat(awaitData().fileName).isEqualTo(fileName)

            underTest.validateFileName()
            assertThat(awaitData().validationSuccessEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.onValidationSuccessEventConsumed()
            assertThat(awaitData().validationSuccessEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }
}
