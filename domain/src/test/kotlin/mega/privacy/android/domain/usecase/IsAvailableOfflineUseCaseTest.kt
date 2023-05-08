package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class IsAvailableOfflineUseCaseTest {
    private lateinit var underTest: IsAvailableOfflineUseCase


    private val file = mock<File>()
    private val nodeId = NodeId(34)
    private val nodeModifiedDateInSeconds = 10L
    private val fileNode = mock<DefaultTypedFileNode> {
        on { id }.thenReturn(nodeId)
        on { modificationTime }.thenReturn(nodeModifiedDateInSeconds)
    }
    private val folderNode = mock<DefaultTypedFolderNode> {
        on { id }.thenReturn(nodeId)
    }
    private val offlineNodeInformation = mock<IncomingShareOfflineNodeInformation>()
    private val nodeRepository = mock<NodeRepository> {
        onBlocking { getOfflineNodeInformation(nodeId) }.thenReturn(
            offlineNodeInformation
        )
    }
    private val getOfflineFile = mock<GetOfflineFileUseCase> {
        onBlocking { invoke(offlineNodeInformation) }.thenReturn(file)
    }

    @Before
    fun setUp() {
        underTest = IsAvailableOfflineUseCase(
            nodeRepository = nodeRepository,
            getOfflineFile = getOfflineFile,
        )
    }

    @Test
    fun `test that false is returned if no offline information is found for the node`() = runTest {
        val node = mock<DefaultTypedFileNode> {
            on { id }.thenReturn(NodeId(56))
        }
        assertThat(underTest(node)).isFalse()
    }

    @Test
    fun `test that false is returned if offline information is returned but file does not exist`() =
        runTest {
            whenever(file.exists()).thenReturn(false)

            assertThat(underTest(fileNode)).isFalse()
        }

    @Test
    fun `test that false is returned if offline file exists, but is older than the node's latest modified date if the node is a file`() =
        runTest {
            whenever(file.exists()).thenReturn(true)
            whenever(file.lastModified()).thenReturn((nodeModifiedDateInSeconds * 1000) - 1)

            assertThat(underTest(fileNode)).isFalse()
        }

    @Test
    fun `test that true is returned if local file exists and the node is a folder`() = runTest {
        whenever(file.exists()).thenReturn(true)

        assertThat(underTest(folderNode)).isTrue()
    }

    @Test
    fun `test that true is returned if offline file exists, and is not older than the node's latest modified date if the node is a file`() =
        runTest {
            whenever(file.exists()).thenReturn(true)
            whenever(file.lastModified()).thenReturn((nodeModifiedDateInSeconds * 1000) + 1)

            assertThat(underTest(fileNode)).isTrue()
        }
}