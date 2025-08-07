package mega.privacy.android.core.nodecomponents.dialog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.INVALID_CHARACTERS
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialogViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidateNodeNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewFolderNodeDialogViewModelTest {
    private lateinit var viewModel: NewFolderNodeDialogViewModel

    private val validateFolderNameUseCase = mock<ValidateNodeNameUseCase>()
    private val createFolderNodeUseCase = mock<CreateFolderNodeUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()

    @BeforeEach
    fun setUp() {
        viewModel = NewFolderNodeDialogViewModel(
            validateNodeNameUseCase = validateFolderNameUseCase,
            createFolderNodeUseCase = createFolderNodeUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            validateFolderNameUseCase,
            createFolderNodeUseCase,
            getRootNodeUseCase
        )
    }

    @Test
    fun `test that createFolder with empty name triggers EmptyNodeNameException`() = runTest {
        val parentNodeId = NodeId(123L)

        whenever(validateFolderNameUseCase(eq(""), eq(parentNodeId))).thenThrow(
            EmptyNodeNameException()
        )

        viewModel.createFolder("", parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isInstanceOf(EmptyNodeNameException::class.java)
        assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
    }

    @Test
    fun `test that createFolder with invalid characters triggers InvalidNodeNameException`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val folderNameWithInvalidChars = "test*folder"

            whenever(validateFolderNameUseCase(eq(folderNameWithInvalidChars), eq(parentNodeId)))
                .thenThrow(InvalidNodeNameException())

            viewModel.createFolder(folderNameWithInvalidChars, parentNodeId)

            val state = viewModel.uiState.value
            assertThat(state.errorEvent.triggeredContent()).isInstanceOf(InvalidNodeNameException::class.java)
            assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
        }

    @Test
    fun `test that createFolder with existing folder name triggers NodeNameAlreadyExistsException`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val folderName = "existingFolder"

            whenever(validateFolderNameUseCase(eq(folderName), eq(parentNodeId)))
                .thenThrow(NodeNameAlreadyExistsException())

            viewModel.createFolder(folderName, parentNodeId)

            val state = viewModel.uiState.value
            assertThat(state.errorEvent.triggeredContent()).isInstanceOf(
                NodeNameAlreadyExistsException::class.java
            )
            assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
        }

    @Test
    fun `test that createFolder with valid name creates folder successfully`() = runTest {
        val parentNodeId = NodeId(123L)
        val folderName = "newFolder"
        val newFolderId = NodeId(456L)

        whenever(validateFolderNameUseCase(eq(folderName), eq(parentNodeId))).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderName), eq(parentNodeId))).thenReturn(newFolderId)

        viewModel.createFolder(folderName, parentNodeId)
        viewModel.uiState.test {
            val state = awaitItem() // Wait for the state after folder creation
            assertThat(state.errorEvent.triggeredContent()).isNull()
            assertThat(state.folderCreatedEvent.triggeredContent()).isEqualTo(newFolderId)
        }

        verify(validateFolderNameUseCase).invoke(eq(folderName), eq(parentNodeId))
        verify(createFolderNodeUseCase).invoke(eq(folderName), eq(parentNodeId))
    }

    @Test
    fun `test that createFolder with valid name trims whitespace`() = runTest {
        val parentNodeId = NodeId(123L)
        val folderNameWithSpaces = "  newFolder  "
        val trimmedFolderName = "newFolder"
        val newFolderId = NodeId(456L)

        whenever(
            validateFolderNameUseCase(
                eq(trimmedFolderName),
                eq(parentNodeId)
            )
        ).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderNameWithSpaces), eq(parentNodeId))).thenReturn(
            newFolderId
        )

        viewModel.createFolder(folderNameWithSpaces, parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isNull()
        assertThat(state.folderCreatedEvent.triggeredContent()).isEqualTo(newFolderId)
        verify(validateFolderNameUseCase).invoke(eq(trimmedFolderName), eq(parentNodeId))
        verify(createFolderNodeUseCase).invoke(eq(folderNameWithSpaces), eq(parentNodeId))
    }

    @Test
    fun `test that createFolder uses root node when parentNodeId is -1`() = runTest {
        val parentNodeId = NodeId(-1L)
        val folderName = "newFolder"
        val rootNodeId = NodeId(789L)
        val newFolderId = NodeId(456L)
        val rootNode = mock<TypedNode>()

        whenever(rootNode.id).thenReturn(rootNodeId)
        whenever(getRootNodeUseCase()).thenReturn(rootNode)
        whenever(validateFolderNameUseCase(eq(folderName), eq(rootNodeId))).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderName), eq(rootNodeId))).thenReturn(newFolderId)

        viewModel.createFolder(folderName, parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isNull()
        assertThat(state.folderCreatedEvent.triggeredContent()).isEqualTo(newFolderId)
        verify(getRootNodeUseCase).invoke()
        verify(validateFolderNameUseCase).invoke(eq(folderName), eq(rootNodeId))
        verify(createFolderNodeUseCase).invoke(eq(folderName), eq(rootNodeId))
    }

    @Test
    fun `test that createFolder handles null root node when parentNodeId is -1`() = runTest {
        val parentNodeId = NodeId(-1L)
        val folderName = "newFolder"

        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(validateFolderNameUseCase(eq(folderName), eq(null))).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderName), eq(null))).thenReturn(null)

        viewModel.createFolder(folderName, parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isNull()
        assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
        verify(getRootNodeUseCase).invoke()
        verify(validateFolderNameUseCase).invoke(eq(folderName), eq(null))
        verify(createFolderNodeUseCase).invoke(eq(folderName), eq(null))
    }

    @Test
    fun `test that createFolder handles CreateFolderNodeUseCase failure gracefully`() = runTest {
        val parentNodeId = NodeId(123L)
        val folderName = "newFolder"

        whenever(validateFolderNameUseCase(eq(folderName), eq(parentNodeId))).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderName), eq(parentNodeId)))
            .thenThrow(RuntimeException("Creation failed"))

        viewModel.createFolder(folderName, parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isNull()
        assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
        verify(validateFolderNameUseCase).invoke(eq(folderName), eq(parentNodeId))
        verify(createFolderNodeUseCase).invoke(eq(folderName), eq(parentNodeId))
    }

    @Test
    fun `test that createFolder handles ValidateFolderNameUseCase failure`() = runTest {
        val parentNodeId = NodeId(123L)
        val folderName = "newFolder"

        whenever(validateFolderNameUseCase(eq(folderName), eq(parentNodeId)))
            .thenThrow(RuntimeException("Validation failed"))

        viewModel.createFolder(folderName, parentNodeId)

        val state = viewModel.uiState.value
        assertThat(state.errorEvent.triggeredContent()).isInstanceOf(RuntimeException::class.java)
        assertThat(state.folderCreatedEvent.triggeredContent()).isNull()
        verify(validateFolderNameUseCase).invoke(eq(folderName), eq(parentNodeId))
    }

    @Test
    fun `test that clearError resets error state`() = runTest {
        val parentNodeId = NodeId(123L)

        // First trigger an error
        whenever(validateFolderNameUseCase(eq(""), eq(parentNodeId))).thenThrow(
            EmptyNodeNameException()
        )
        viewModel.createFolder("", parentNodeId)
        assertThat(viewModel.uiState.value.errorEvent.triggeredContent()).isInstanceOf(
            EmptyNodeNameException::class.java
        )

        // Clear the error
        viewModel.clearError()

        // Verify error is cleared
        assertThat(viewModel.uiState.value.errorEvent.triggeredContent()).isNull()
    }

    @Test
    fun `test that clearFolderCreatedEvent resets folder created state`() = runTest {
        val parentNodeId = NodeId(123L)
        val folderName = "newFolder"
        val newFolderId = NodeId(456L)

        whenever(validateFolderNameUseCase(eq(folderName), eq(parentNodeId))).thenReturn(Unit)
        whenever(createFolderNodeUseCase(eq(folderName), eq(parentNodeId))).thenReturn(newFolderId)

        viewModel.createFolder(folderName, parentNodeId)
        assertThat(viewModel.uiState.value.folderCreatedEvent.triggeredContent()).isEqualTo(
            newFolderId
        )

        viewModel.clearFolderCreatedEvent()

        assertThat(viewModel.uiState.value.folderCreatedEvent.triggeredContent()).isNull()
    }

    @Test
    fun `test that INVALID_CHARACTERS constant contains correct characters`() {
        val expectedInvalidChars = "\" * / : < > ? \\ |"
        assertThat(INVALID_CHARACTERS).isEqualTo(expectedInvalidChars)
    }

    fun <T> StateEventWithContent<T>.triggeredContent(): T? =
        (this as? StateEventWithContentTriggered<T>)?.content
}