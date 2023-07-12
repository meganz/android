package mega.privacy.android.domain.usecase.filelink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.repository.FileLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetPublicNodeUseCaseTest {
    private lateinit var underTest: GetPublicNodeUseCase
    private val repository: FileLinkRepository = mock()
    private val addNodeType: AddNodeType = mock()

    @Before
    fun setUp() {
        underTest = GetPublicNodeUseCase(repository, addNodeType)
    }

    @Test
    fun `test that valid file node is returned`() = runTest {
        val url = "https://mega.co.nz/abc"
        val typedFileNode = mock<TypedFileNode>()
        val untypedNode = mock<FileNode>()

        whenever(repository.getPublicNode(url)).thenReturn(untypedNode)
        whenever(addNodeType(untypedNode)).thenReturn(typedFileNode)

        assertThat(underTest(url)).isEqualTo(typedFileNode)
    }

    @Test
    fun `test that GenericError exception is thrown on getting incorrect untyped node`() = runTest {
        val url = "https://mega.co.nz/abc"
        val typedFolderNode = mock<TypedFolderNode>()
        val untypedNode = mock<FolderNode>()

        whenever(repository.getPublicNode(url)).thenReturn(untypedNode)
        whenever(addNodeType(untypedNode)).thenReturn(typedFolderNode)

        assertThrows<PublicNodeException.GenericError> { underTest(url) }
    }
}