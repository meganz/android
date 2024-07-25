package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeContentUriByHandleUseCaseTest {
    private lateinit var underTest: GetNodeContentUriByHandleUseCase

    private val getFolderLinkNodeContentUriUseCase = mock<GetFolderLinkNodeContentUriUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val addNodeType = mock<AddNodeType>()

    private val paramHandle = 1L
    private val expectedNodeContentUri = NodeContentUri.RemoteContentUri("url", true)

    @BeforeAll
    fun setup() {
        underTest = GetNodeContentUriByHandleUseCase(
            getFolderLinkNodeContentUriUseCase = getFolderLinkNodeContentUriUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            addNodeType = addNodeType
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFolderLinkNodeContentUriUseCase,
            getNodeByHandleUseCase,
            addNodeType
        )
    }

    @Test
    fun `test that the returned result is expected`() = runTest {
        val fileNode = mock<FileNode>()
        val typedFileNode = mock<TypedFileNode>()
        whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(fileNode)
        whenever(addNodeType(fileNode)).thenReturn(typedFileNode)
        whenever(getFolderLinkNodeContentUriUseCase(typedFileNode)).thenReturn(
            expectedNodeContentUri
        )
        val actual = underTest(paramHandle)
        assertThat(actual).isEqualTo(expectedNodeContentUri)
    }

    @Test
    fun `test that the result returns null when the node is null`() = runTest {
        val typedFileNode = mock<TypedFileNode>()
        whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(null)
        whenever(getFolderLinkNodeContentUriUseCase(typedFileNode)).thenReturn(
            expectedNodeContentUri
        )
        assertFailsWith<IllegalStateException>("cannot find node") {
            underTest(paramHandle)
        }
    }

    @Test
    fun `test that the result returns null when the node is not a file`() = runTest {
        val typedFolderNode = mock<TypedFolderNode>()
        val folderNode = mock<FolderNode>()
        whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(folderNode)
        whenever(addNodeType(folderNode)).thenReturn(typedFolderNode)
        whenever(getFolderLinkNodeContentUriUseCase(anyOrNull())).thenReturn(expectedNodeContentUri)
        assertFailsWith<IllegalStateException>("node is not a file") {
            underTest(paramHandle)
        }
    }
}