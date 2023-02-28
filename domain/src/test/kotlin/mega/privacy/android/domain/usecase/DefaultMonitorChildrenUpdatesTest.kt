package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorChildrenUpdatesTest {

    private lateinit var underTest: MonitorChildrenUpdates
    private val nodeRepository = mock<NodeRepository>()
    private val folder = mock<FolderNode> {
        on { id }.thenReturn(folderId)
    }
    private val childNode = mock<Node> {
        on { id }.thenReturn(childId)
        on { parentId }.thenReturn(folderId)
    }
    private val otherNode = mock<Node> {
        on { id }.thenReturn(NodeId(2L))
    }

    @Before
    fun setUp() {
        underTest = DefaultMonitorChildrenUpdates(nodeRepository = nodeRepository)
    }


    @Test
    fun `test only child node updates are emitted`() = runTest {
        val expectedChanges1 = listOf(NodeChanges.Name, NodeChanges.Parent)
        val expectedChanges2 = listOf(NodeChanges.Owner, NodeChanges.Parent)
        val otherChanges1 = listOf(NodeChanges.Name, NodeChanges.Owner)
        val otherChanges2 = listOf(NodeChanges.Owner, NodeChanges.Favourite)
        val expectedUpdate1 = mapOf(childNode to expectedChanges1, otherNode to otherChanges1)
        val filteredUpdate1 = mapOf(otherNode to otherChanges1)
        val filteredUpdate2 = mapOf(folder as Node to otherChanges2)
        val expectedUpdate2 = mapOf(childNode to expectedChanges2, otherNode to otherChanges2)
        whenever(nodeRepository.getNodeById(folderId)).thenReturn(folder)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
            flowOf(
                NodeUpdate(expectedUpdate1),
                NodeUpdate(filteredUpdate1),
                NodeUpdate(expectedUpdate2),
                NodeUpdate(filteredUpdate2),
            )
        )
        underTest.invoke(folder.id).test {
            val firstChange = awaitItem()
            Truth.assertThat(firstChange.changes.values).containsExactly(expectedChanges1)
            Truth.assertThat(firstChange.changes.keys).hasSize(1)
            Truth.assertThat(firstChange.changes.keys.first().id).isEqualTo(childId)
            val secondChange = awaitItem()
            Truth.assertThat(secondChange.changes.values).containsExactly(expectedChanges2)
            Truth.assertThat(secondChange.changes.keys).hasSize(1)
            Truth.assertThat(secondChange.changes.keys.first().id).isEqualTo(childId)
            awaitComplete()
        }
    }

    private companion object {
        val folderId = NodeId(1L)
        val childId = NodeId(11L)
    }
}