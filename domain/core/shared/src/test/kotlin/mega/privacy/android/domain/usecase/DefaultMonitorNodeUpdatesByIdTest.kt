package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
class DefaultMonitorNodeUpdatesByIdTest {

    private lateinit var underTest: MonitorNodeUpdatesById
    private val nodeRepository = mock<NodeRepository>()
    private val node = mock<Node> {
        on { id }.thenReturn(id)
    }
    private val otherNode = mock<Node> {
        on { id }.thenReturn(NodeId(2L))
    }

    @Before
    fun setUp() {
        underTest = DefaultMonitorNodeUpdatesById(nodeRepository = nodeRepository)
    }


    @Test
    fun `test only selected node updates are emitted`() = runTest {
        val expectedChanges1 = listOf(NodeChanges.Name, NodeChanges.Parent)
        val expectedChanges2 = listOf(NodeChanges.Owner, NodeChanges.Parent)
        val otherChanges1 = listOf(NodeChanges.Name, NodeChanges.Owner)
        val otherChanges2 = listOf(NodeChanges.Owner, NodeChanges.Favourite)
        val expectedUpdate1 = mapOf(node to expectedChanges1, otherNode to otherChanges1)
        val filteredUpdate = mapOf(otherNode to otherChanges2)
        val expectedUpdate2 = mapOf(node to expectedChanges2)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
            flowOf(
                NodeUpdate(expectedUpdate1),
                NodeUpdate(filteredUpdate),
                NodeUpdate(expectedUpdate2)
            )
        )
        underTest.invoke(id).test {
            Truth.assertThat(awaitItem()).containsExactlyElementsIn(expectedChanges1)
            Truth.assertThat(awaitItem()).containsExactlyElementsIn(expectedChanges2)
            awaitComplete()
        }
    }

    private companion object {
        val id = NodeId(1L)
    }
}