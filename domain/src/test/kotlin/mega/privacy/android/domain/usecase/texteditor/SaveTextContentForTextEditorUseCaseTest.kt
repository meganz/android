package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            getRootNodeUseCase,
            getNodeNameCollisionRenameNameUseCase,
            getCacheFileUseCase,
            fileSystemRepository,
        )
    }

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
        whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

        val saveResult = underTest(
            nodeHandle = nodeHandle,
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
        verify(fileSystemRepository).writeTextToPath(tempFile.absolutePath, "content")
        verifyNoInteractions(getNodeNameCollisionRenameNameUseCase)
    }

    @Test
    fun `test that save returns UploadRequired when mode is Create with folder as parent`() =
        runTest {
            val folderHandle = 5L
            val folderNode = mock<TypedFolderNode> {
                on { id } doReturn NodeId(folderHandle)
            }
            val tempFile = File(tempDir, "new-in-folder.txt")
            whenever(getNodeByIdUseCase(NodeId(folderHandle))).thenReturn(folderNode)
            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("new-in-folder.txt")
            whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

            val saveResult = underTest(
                nodeHandle = folderHandle,
                text = "content in folder",
                fileName = "new-in-folder.txt",
                mode = TextEditorMode.Create,
                fromHome = false,
            )

            assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
            (saveResult as TextEditorSaveResult.UploadRequired).let {
                assertThat(it.tempPath).isEqualTo(tempFile.absolutePath)
                assertThat(it.parentHandle).isEqualTo(folderHandle)
                assertThat(it.isEditMode).isFalse()
                assertThat(it.fromHome).isFalse()
            }
            verify(fileSystemRepository).writeTextToPath(tempFile.absolutePath, "content in folder")
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
    fun `test that save when Edit and isFromSharedFolder uses unique name from collision use case`() =
        runTest {
            val parentHandle = 100L
            val nodeHandle = 99L
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(nodeHandle)
                on { parentId } doReturn NodeId(parentHandle)
            }
            val tempFile = File(tempDir, "notes (1).txt")
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("notes (1).txt")
            whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

            val saveResult = underTest(
                nodeHandle = nodeHandle,
                text = "content",
                fileName = "notes.txt",
                mode = TextEditorMode.Edit,
                fromHome = false,
                isFromSharedFolder = true,
            )

            assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
            (saveResult as TextEditorSaveResult.UploadRequired).let {
                assertThat(it.tempPath).isEqualTo(tempFile.absolutePath)
                assertThat(it.parentHandle).isEqualTo(parentHandle)
                assertThat(it.isEditMode).isTrue()
            }
            verify(getNodeNameCollisionRenameNameUseCase).invoke(any())
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
        whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

        val saveResult = underTest(
            nodeHandle = nodeHandle,
            text = "content",
            fileName = "",
            mode = TextEditorMode.Edit,
            fromHome = false,
        )

        assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
        assertThat((saveResult as TextEditorSaveResult.UploadRequired).tempPath).isEqualTo(tempFile.absolutePath)
        verifyNoInteractions(getNodeNameCollisionRenameNameUseCase)
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

    @Test
    fun `test that save when Edit and not from shared folder does not call collision rename use case`() =
        runTest {
            val parentHandle = 100L
            val nodeHandle = 99L
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(nodeHandle)
                on { parentId } doReturn NodeId(parentHandle)
            }
            val tempFile = File(tempDir, "notes.txt")
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
            whenever(getCacheFileUseCase(any(), any())).thenReturn(tempFile)

            underTest(
                nodeHandle = nodeHandle,
                text = "content",
                fileName = "notes.txt",
                mode = TextEditorMode.Edit,
                isFromSharedFolder = false,
            )

            verifyNoInteractions(getNodeNameCollisionRenameNameUseCase)
        }

    @Test
    fun `test that save deletes stale directory and writes file when Edit and cache path is directory`() =
        runTest {
            val parentHandle = 100L
            val nodeHandle = 99L
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(nodeHandle)
                on { parentId } doReturn NodeId(parentHandle)
            }
            val dirAsFile = File(tempDir, "notes.txt")
            dirAsFile.mkdirs()
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
            whenever(getCacheFileUseCase(any(), any())).thenReturn(dirAsFile)

            val saveResult = underTest(
                nodeHandle = nodeHandle,
                text = "content",
                fileName = "notes.txt",
                mode = TextEditorMode.Edit,
                isFromSharedFolder = false,
            )

            verify(fileSystemRepository).deleteFolderAndItsFiles(dirAsFile.absolutePath)
            assertThat(saveResult).isInstanceOf(TextEditorSaveResult.UploadRequired::class.java)
        }
}
