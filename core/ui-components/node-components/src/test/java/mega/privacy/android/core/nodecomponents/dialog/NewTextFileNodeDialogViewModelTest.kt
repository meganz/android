package mega.privacy.android.core.nodecomponents.dialog

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialogViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
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
class NewTextFileNodeDialogViewModelTest {

    private lateinit var viewModel: NewTextFileNodeDialogViewModel

    private val validateNodeNameUseCase = mock<ValidateNodeNameUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()

    @BeforeEach
    fun setUp() {
        viewModel = NewTextFileNodeDialogViewModel(
            validateNodeNameUseCase = validateNodeNameUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            validateNodeNameUseCase,
            getRootNodeUseCase
        )
    }

    @Test
    fun `test that createTextFile with empty name triggers EmptyNodeNameException`() = runTest {
        val parentNodeId = NodeId(123L)

        whenever(validateNodeNameUseCase(eq(""), eq(parentNodeId))).thenThrow(
            EmptyNodeNameException()
        )

        val result = viewModel.createTextFile("", parentNodeId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(EmptyNodeNameException::class.java)
    }

    @Test
    fun `test that createTextFile with whitespace name triggers EmptyNodeNameException`() =
        runTest {
            val parentNodeId = NodeId(123L)

            whenever(validateNodeNameUseCase(eq(""), eq(parentNodeId))).thenThrow(
                EmptyNodeNameException()
            )

            val result = viewModel.createTextFile("   ", parentNodeId)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(EmptyNodeNameException::class.java)
        }

    @Test
    fun `test that createTextFile with invalid characters triggers InvalidNodeNameException`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val fileNameWithInvalidChars = "test*folder"

            whenever(validateNodeNameUseCase(eq(fileNameWithInvalidChars), eq(parentNodeId)))
                .thenThrow(InvalidNodeNameException())

            val result = viewModel.createTextFile(fileNameWithInvalidChars, parentNodeId)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(InvalidNodeNameException::class.java)
        }

    @Test
    fun `test that createTextFile with existing file name triggers NodeNameAlreadyExistsException`() =
        runTest {
            val parentNodeId = NodeId(123L)
            val fileName = "existingFile.txt"

            whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId)))
                .thenThrow(NodeNameAlreadyExistsException())

            val result = viewModel.createTextFile(fileName, parentNodeId)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(NodeNameAlreadyExistsException::class.java)
        }

    @Test
    fun `test that createTextFile with valid name creates text file successfully`() = runTest {
        val parentNodeId = NodeId(123L)
        val fileName = "newFile.txt"

        whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId))).thenReturn(Unit)

        val result = viewModel.createTextFile(fileName, parentNodeId)

        assertThat(result.isSuccess).isTrue()
        val (actualParentNodeId, actualFileName) = result.getOrThrow()
        assertThat(actualFileName).isEqualTo(fileName)
        assertThat(actualParentNodeId).isEqualTo(parentNodeId)
        verify(validateNodeNameUseCase).invoke(eq(fileName), eq(parentNodeId))
    }

    @Test
    fun `test that createTextFile with valid name trims whitespace`() = runTest {
        val parentNodeId = NodeId(123L)
        val fileNameWithSpaces = "  newFile.txt  "
        val trimmedFileName = "newFile.txt"

        whenever(validateNodeNameUseCase(eq(trimmedFileName), eq(parentNodeId))).thenReturn(Unit)

        val result = viewModel.createTextFile(fileNameWithSpaces, parentNodeId)

        assertThat(result.isSuccess).isTrue()
        val (actualParentNodeId, actualFileName) = result.getOrThrow()
        assertThat(actualFileName).isEqualTo(trimmedFileName)
        assertThat(actualParentNodeId).isEqualTo(parentNodeId)
        verify(validateNodeNameUseCase).invoke(eq(trimmedFileName), eq(parentNodeId))
    }

    @Test
    fun `test that createTextFile uses root node when parentNodeId is -1`() = runTest {
        val parentNodeId = NodeId(-1L)
        val fileName = "newFile.txt"
        val rootNodeId = NodeId(789L)
        val rootNode = mock<TypedNode>()

        whenever(rootNode.id).thenReturn(rootNodeId)
        whenever(getRootNodeUseCase()).thenReturn(rootNode)
        whenever(validateNodeNameUseCase(eq(fileName), eq(rootNodeId))).thenReturn(Unit)

        val result = viewModel.createTextFile(fileName, parentNodeId)

        assertThat(result.isSuccess).isTrue()
        val (actualParentNodeId, actualFileName) = result.getOrThrow()
        assertThat(actualParentNodeId).isEqualTo(rootNodeId)
        assertThat(actualFileName).isEqualTo(fileName)
        verify(getRootNodeUseCase).invoke()
        verify(validateNodeNameUseCase).invoke(eq(fileName), eq(rootNodeId))
    }

    @Test
    fun `test that createTextFile throws when root node is null`() = runTest {
        val parentNodeId = NodeId(-1L)
        val fileName = "newFile.txt"

        whenever(getRootNodeUseCase()).thenReturn(null)

        val result = viewModel.createTextFile(fileName, parentNodeId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        verify(getRootNodeUseCase).invoke()
    }

    @Test
    fun `test that createTextFile handles ValidateNodeNameUseCase failure`() = runTest {
        val parentNodeId = NodeId(123L)
        val fileName = "newFile.txt"

        whenever(validateNodeNameUseCase(eq(fileName), eq(parentNodeId)))
            .thenThrow(RuntimeException("Validation failed"))

        val result = viewModel.createTextFile(fileName, parentNodeId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        verify(validateNodeNameUseCase).invoke(eq(fileName), eq(parentNodeId))
    }

    fun <T> StateEventWithContent<T>.triggeredContent(): T? =
        (this as? StateEventWithContentTriggered<T>)?.content
} 