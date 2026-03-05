package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveTextContentForTextEditorUseCaseTest {

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase =
        mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    private val underTest = SaveTextContentForTextEditorUseCase(
        ioDispatcher = ioDispatcher,
        getNodeByIdUseCase = getNodeByIdUseCase,
        getRootNodeUseCase = getRootNodeUseCase,
        getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase,
        getCacheFileUseCase = getCacheFileUseCase,
        fileSystemRepository = fileSystemRepository,
    )

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test that save returns UploadRequired when mode is Edit and node exists`() = runTest {
        val parentHandle = 100L
        val nodeHandle = 99L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { parentId } doReturn NodeId(parentHandle)
        }
        val tempFile = File(tempDir, "notes.txt")
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("notes.txt")
        whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

        val saveResult = underTest(
            nodeHandle = nodeHandle,
            nodeSourceType = 0,
            text = "content",
            fileName = "notes.txt",
            mode = TextEditorMode.Edit,
            fromHome = false,
        )

        assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
        (saveResult as TextEditorSaveResult.UploadRequired).let {
            assertThat(it.tempPath).isEqualTo(tempFile.absolutePath)
            assertThat(it.parentHandle).isEqualTo(parentHandle)
            assertThat(it.isEditMode).isTrue()
            assertThat(it.fromHome).isFalse()
        }
    }

    @Test
    fun `test that save returns UploadRequired when mode is Create with root and fromHome true`() =
        runTest {
            val rootHandle = 1L
            val rootNode = mock<Node> {
                on { id } doReturn NodeId(rootHandle)
            }
            val tempFile = File(tempDir, "new.txt")
            whenever(getRootNodeUseCase()).thenReturn(rootNode)
            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("new.txt")
            whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

            val saveResult = underTest(
                nodeHandle = 0L,
                nodeSourceType = 0,
                text = "new content",
                fileName = "new.txt",
                mode = TextEditorMode.Create,
                fromHome = true,
            )

            assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
            (saveResult as TextEditorSaveResult.UploadRequired).let {
                assertThat(it.tempPath).isEqualTo(tempFile.absolutePath)
                assertThat(it.parentHandle).isEqualTo(rootHandle)
                assertThat(it.isEditMode).isFalse()
                assertThat(it.fromHome).isTrue()
            }
        }

    @Test
    fun `test that save throws when mode is View`() = runTest {
        val result = runCatching {
            underTest(
                nodeHandle = 1L,
                nodeSourceType = 0,
                text = "content",
                fileName = "a.txt",
                mode = TextEditorMode.View,
            )
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Could not resolve parent handle")
    }

    @Test
    fun `test that save throws when mode is Edit and node not found`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(999L))).thenReturn(null)

        val result = runCatching {
            underTest(
                nodeHandle = 999L,
                nodeSourceType = 0,
                text = "content",
                fileName = "a.txt",
                mode = TextEditorMode.Edit,
            )
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Could not resolve parent handle")
    }

    @Test
    fun `test that save uses untitled when fileName is empty`() = runTest {
        val parentHandle = 100L
        val nodeHandle = 99L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { parentId } doReturn NodeId(parentHandle)
        }
        val tempFile = File(tempDir, "untitled.txt")
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("untitled.txt")
        whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

        val saveResult = underTest(
            nodeHandle = nodeHandle,
            nodeSourceType = 0,
            text = "content",
            fileName = "",
            mode = TextEditorMode.Edit,
            fromHome = false,
        )

        assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
        assertThat((saveResult as TextEditorSaveResult.UploadRequired).tempPath).isEqualTo(tempFile.absolutePath)
    }

    @Test
    fun `test that save throws when getCacheFileUseCase returns null`() = runTest {
        val parentHandle = 100L
        val nodeHandle = 99L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { parentId } doReturn NodeId(parentHandle)
        }
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("notes.txt")
        whenever(getCacheFileUseCase(any(), any())).thenReturn(null)

        val result = runCatching {
            underTest(
                nodeHandle = nodeHandle,
                nodeSourceType = 0,
                text = "content",
                fileName = "notes.txt",
                mode = TextEditorMode.Edit,
            )
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Cannot get temp file")
    }
}
