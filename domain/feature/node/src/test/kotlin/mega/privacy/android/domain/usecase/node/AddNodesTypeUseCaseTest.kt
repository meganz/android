package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddNodesTypeUseCaseTest {

    private lateinit var underTest: AddNodesTypeUseCase

    private val getFolderTypeDataUseCase = mock<GetFolderTypeDataUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    private val folderTypeData = FolderTypeData(
        primarySyncHandle = null,
        secondarySyncHandle = null,
        chatFilesFolderId = null,
        backupFolderId = null,
        backupFolderPath = null,
        syncedNodeIds = emptySet(),
    )

    @BeforeAll
    fun setUp() {
        underTest = AddNodesTypeUseCase(
            getFolderTypeDataUseCase = getFolderTypeDataUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFolderTypeDataUseCase, nodeRepository)
    }

    private fun stubFolderNode(handle: Long = 1L) = mock<FolderNode> {
        on { id }.thenReturn(NodeId(handle))
        on { parentId }.thenReturn(NodeId(handle + 1))
    }

    @Test
    fun `test that invoke returns already typed nodes unchanged`() = runTest {
        val typedNode = mock<DefaultTypedFolderNode> {
            on { id }.thenReturn(NodeId(1))
        }
        val nodes = listOf(typedNode)

        val result = underTest(nodes)

        assertThat(result).isEqualTo(nodes)
    }

    @Test
    fun `test that invoke maps file nodes to DefaultTypedFileNode`() = runTest {
        val fileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(1))
        }

        val result = underTest(listOf(fileNode))

        assertThat(result.first()).isInstanceOf(DefaultTypedFileNode::class.java)
    }

    @Test
    fun `test that invoke fetches folder type data only once when list has multiple folder nodes`() =
        runTest {
            whenever(getFolderTypeDataUseCase()).thenReturn(folderTypeData)
            whenever(nodeRepository.getFolderType(any(), any())).thenReturn(FolderType.Default)
            val nodes = (1L..50L).map { stubFolderNode(handle = it) }

            underTest(nodes)

            verify(getFolderTypeDataUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke does not fetch folder type data when list has no untyped folder nodes`() =
        runTest {
            val fileNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1))
            }
            val typedFolder = mock<DefaultTypedFolderNode> {
                on { id }.thenReturn(NodeId(2))
            }

            underTest(listOf(fileNode, typedFolder))

            verify(getFolderTypeDataUseCase, never()).invoke()
            verify(nodeRepository, never()).getFolderType(any(), any())
        }

    @Test
    fun `test that invoke maps folder nodes with the type returned by the repository`() = runTest {
        whenever(getFolderTypeDataUseCase()).thenReturn(folderTypeData)
        val folderNode = stubFolderNode(handle = 1L)
        whenever(nodeRepository.getFolderType(folderNode, folderTypeData))
            .thenReturn(FolderType.ChatFilesFolder)

        val result = underTest(listOf(folderNode))

        assertThat((result.first() as DefaultTypedFolderNode).type)
            .isEqualTo(FolderType.ChatFilesFolder)
    }

    @Test
    fun `test that invoke passes the prefetched folder type data to the repository for every folder node`() =
        runTest {
            whenever(getFolderTypeDataUseCase()).thenReturn(folderTypeData)
            whenever(nodeRepository.getFolderType(any(), any())).thenReturn(FolderType.Default)
            val nodes = (1L..3L).map { stubFolderNode(handle = it) }

            underTest(nodes)

            verify(nodeRepository, times(3)).getFolderType(any(), eq(folderTypeData))
        }
}
