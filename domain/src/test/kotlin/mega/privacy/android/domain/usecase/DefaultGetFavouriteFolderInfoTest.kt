package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetFavouriteFolderInfoTest {
    lateinit var underTest: GetFavouriteFolderInfo

    private val fileRepository = mock<FileRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFavouriteFolderInfo(fileRepository = fileRepository)
        whenever(fileRepository.monitorNodeUpdates()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that a non folder node throws an exception`() = runTest {
        val fileNode = mock<FileNode>()
        val nodeId = NodeId(1)
        whenever(fileRepository.getNodeById(nodeId)).thenReturn(fileNode)
        underTest(nodeId.id).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(ParentNotAFolderException::class.java)
        }
    }

    @Test
    fun `test that subsequent items are returned when nodes update`() = runTest {
        whenever(fileRepository.monitorNodeUpdates()).thenReturn(flowOf(emptyList()))
        val first = "first"
        val second = "second"
        val folderNode = mock<FolderNode> {
            on { name }.thenReturn(first, second)
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodeId = NodeId(1)
        whenever(fileRepository.getNodeById(nodeId)).thenReturn(folderNode)
        whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(emptyList())

        underTest(nodeId.id).test {
            assertThat(awaitItem()?.name).isEqualTo(first)
            assertThat(awaitItem()?.name).isEqualTo(second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that children are returned if present`() = runTest {
        val folderNode = mock<FolderNode> {
            on { name }.thenReturn("Name")
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodeId = NodeId(1)
        val children = listOf(mock<FileNode>())
        whenever(fileRepository.getNodeById(nodeId)).thenReturn(folderNode)
        whenever(fileRepository.getNodeChildren(folderNode)).thenReturn(children)

        underTest(nodeId.id).test {
            assertThat(awaitItem()?.children).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

}