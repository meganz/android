package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
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

    private val nodeId = NodeId(34)
    private val nodeModifiedDateInSeconds = 10L

    private val offlineNodeInformation = mock<IncomingShareOfflineNodeInformation>()
    private val nodeRepository = mock<NodeRepository>()

    @Before
    fun setUp() {
        underTest = IsAvailableOfflineUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @Test
    fun `test that false is returned if no offline information is found for the node`() = runTest {
        val node = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(56))
        }
        whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(null)
        assertThat(underTest(node)).isFalse()
    }

    @Test
    fun `test that false is returned if offline information is found but its timestamp is less than last modified timestamp on FileNode`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
                on { modificationTime }.thenReturn(nodeModifiedDateInSeconds)
            }
            whenever(offlineNodeInformation.lastModifiedTime).thenReturn(1L)
            whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(
                offlineNodeInformation
            )
            assertThat(underTest(node)).isFalse()
        }

    @Test
    fun `test that true is returned if offline information is found and its timestamp is equal to last modified timestamp on FileNode`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
                on { modificationTime }.thenReturn(nodeModifiedDateInSeconds)
            }
            whenever(offlineNodeInformation.lastModifiedTime).thenReturn(nodeModifiedDateInSeconds)
            whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(
                offlineNodeInformation
            )
            assertThat(underTest(node)).isTrue()
        }

    @Test
    fun `test that true is returned if offline information is found and its timestamp is greater than last modified timestamp on FileNode`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
                on { modificationTime }.thenReturn(nodeModifiedDateInSeconds)
            }
            whenever(offlineNodeInformation.lastModifiedTime).thenReturn(11L)
            whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(
                offlineNodeInformation
            )
            assertThat(underTest(node)).isTrue()
        }

    @Test
    fun `test that true is returned if offline information is found for folder in offline database`() =
        runTest {
            val node = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
            }
            whenever(offlineNodeInformation.isFolder).thenReturn(true)
            whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(
                offlineNodeInformation
            )
            assertThat(underTest(node)).isTrue()
        }

    @Test
    fun `test that false is returned if offline information is not found for folder in offline database`() =
        runTest {
            val node = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(56L))
            }
            whenever(offlineNodeInformation.isFolder).thenReturn(true)
            whenever(nodeRepository.getOfflineNodeInformation(node.id)).thenReturn(null)
            assertThat(underTest(node)).isFalse()
        }
}