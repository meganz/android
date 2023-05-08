package mega.privacy.android.domain.usecase.folderlink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFolderParentNodeUseCaseTest {
    private lateinit var underTest: GetFolderParentNodeUseCase
    private val repository: FolderLinkRepository = mock()
    private val addNodeType: AddNodeType = mock()
    private val typedFolderNode = mock<TypedFolderNode>()
    private val unTypeNode = mock<FolderNode>()
    private val typedNode = mock<DefaultTypedFileNode>()

    @Before
    fun setUp() {
        underTest = GetFolderParentNodeUseCase(repository, addNodeType)
    }

    @Test
    fun `Test that TypedFolder is returned on valid values`() = runTest {
        val nodeId = NodeId(123)
        val result = typedFolderNode
        whenever(repository.getParentNode(nodeId)).thenReturn(unTypeNode)
        whenever(addNodeType(unTypeNode)).thenReturn(typedFolderNode)

        assertThat(underTest(nodeId)).isEqualTo(result)
    }

    @Test
    fun `Test that exception is thrown on unsuccessful cast `() = runTest {
        val nodeId = NodeId(123)
        whenever(repository.getParentNode(nodeId)).thenReturn(unTypeNode)
        whenever(addNodeType(unTypeNode)).thenReturn(typedNode)

        Assert.assertThrows(FetchFolderNodesException.GenericError::class.java) {
            runBlocking { underTest(nodeId) }
        }
    }
}