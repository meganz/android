package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorFolderLinkNodesUseCaseTest {
    private lateinit var underTest: MonitorFolderLinkNodesUseCase

    private val folderLinkRepository = mock<FolderLinkRepository>()
    private val nodeRepository = mock<NodeRepository>()

    private val parentId = NodeId(1L)

    @Before
    fun setUp() {
        underTest = MonitorFolderLinkNodesUseCase(
            folderLinkRepository = folderLinkRepository,
            nodeRepository = nodeRepository,
        )
    }

    private fun imageNode(id: Long): ImageNode = mock { on { this.id }.thenReturn(NodeId(id)) }

    @Test
    fun `test that initial folder link nodes are emitted`() = runTest {
        val nodes = listOf(imageNode(10L), imageNode(11L))
        whenever(folderLinkRepository.getFolderLinkImageNodes(parentId.longValue, null))
            .thenReturn(nodes)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(emptyFlow())

        underTest(parentId).test {
            assertThat(awaitItem().map { it.id.longValue }).containsExactly(10L, 11L)
            awaitComplete()
        }
    }

    @Test
    fun `test that emission is not repeated when node ids are unchanged`() = runTest {
        // populateNodes is called on the initial emit and again on the node update.
        // Both return distinct instances but with the SAME ids.
        // Build the mock lists up front so mock stubbing isn't nested inside thenReturn(...).
        val first = listOf(imageNode(10L), imageNode(11L))
        val second = listOf(imageNode(10L), imageNode(11L))
        whenever(folderLinkRepository.getFolderLinkImageNodes(parentId.longValue, null))
            .thenReturn(first, second)
        whenever(nodeRepository.monitorNodeUpdates())
            .thenReturn(flowOf(NodeUpdate(emptyMap())))

        underTest(parentId).test {
            assertThat(awaitItem().map { it.id.longValue }).containsExactly(10L, 11L)
            // The second populate produces the same id set, so it must be suppressed.
            awaitComplete()
        }
    }

    @Test
    fun `test that emission happens when node ids change`() = runTest {
        val first = listOf(imageNode(10L), imageNode(11L))
        val second = listOf(imageNode(10L), imageNode(12L))
        whenever(folderLinkRepository.getFolderLinkImageNodes(parentId.longValue, null))
            .thenReturn(first, second)
        whenever(nodeRepository.monitorNodeUpdates())
            .thenReturn(flowOf(NodeUpdate(emptyMap())))

        underTest(parentId).test {
            assertThat(awaitItem().map { it.id.longValue }).containsExactly(10L, 11L)
            assertThat(awaitItem().map { it.id.longValue }).containsExactly(10L, 12L)
            awaitComplete()
        }
    }
}
