package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetFolderTreeInfoTest {
    private lateinit var underTest: GetFolderTreeInfo

    private lateinit var nodeRepository: NodeRepository
    private lateinit var folderNode: FolderNode

    @Before
    fun setUp() {
        nodeRepository = mock()
        folderNode = mock()
        whenever(folderNode.id).thenReturn(nodeId)
        underTest = DefaultGetFolderTreeInfo(nodeRepository)
    }

    @Test
    fun `test nodeRepository is queried to get the FolderVersionInfo`() = runTest {
        underTest.invoke(folderNode)
        verify(nodeRepository).getFolderTreeInfo(folderNode)
    }

    @Test
    fun `test correct FolderVersionInfo is returned`() = runTest {
        whenever(nodeRepository.getFolderTreeInfo(folderNode)).thenReturn(folderInfo)
        val result = underTest.invoke(folderNode)
        Truth.assertThat(result).isEqualTo(folderInfo)
    }

    @Test(expected = java.lang.RuntimeException::class)
    fun `test exception is propagated when nodeRepository launch an error`() = runTest {
        whenever(nodeRepository.getFolderTreeInfo(folderNode)).thenThrow(RuntimeException::class.java)
        val result = underTest.invoke(folderNode)
        Truth.assertThat(result).isNull()
    }

    companion object {
        private val nodeId = NodeId(1L)
        private val folderInfo = FolderTreeInfo(
            numberOfVersions = 2,
            sizeOfPreviousVersionsInBytes = 1000L,
            numberOfFiles = 4,
            numberOfFolders = 2,
            totalCurrentSizeInBytes = 2000L,
        )
    }
}