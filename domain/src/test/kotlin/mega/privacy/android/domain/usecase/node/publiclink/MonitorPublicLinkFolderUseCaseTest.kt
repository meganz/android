package mega.privacy.android.domain.usecase.node.publiclink

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
internal class MonitorPublicLinkFolderUseCaseTest {
    private lateinit var underTest: MonitorPublicLinkFolderUseCase

    private val nodeRepository = mock<NodeRepository> {
        on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
    }

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorPublicLinkFolderUseCase(
            getCloudSortOrder = { SortOrder.ORDER_DEFAULT_ASC },
            nodeRepository = nodeRepository
        )
    }

    @Test
    internal fun `test that children are returned`() = runTest {
        val children = listOf<FileNode>(mock(), mock())
        val parent = mock<FolderNode> {
            onBlocking { fetchChildren }.thenReturn { children }
        }


        underTest(parent).test {
            Truth.assertThat(awaitItem()).hasSize(children.size)
            Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    internal fun `test that new items are emitted if a node update of type PublicLink is emitted`() =
        runTest {
            val children = listOf<FileNode>(mock(), mock())
            val parent = mock<FolderNode> {
                onBlocking { fetchChildren }.thenReturn({ emptyList() }, { children })
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            underTest(parent).test {
                Truth.assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
                Truth.assertThat(awaitItem()).isEqualTo(children)
                Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }

        }

    @Test
    internal fun `test that no new items are emitted if a node update of type not of PublicLink is emitted`() =
        runTest {
            val children = listOf<FileNode>(mock(), mock())
            val parent = mock<FolderNode> {
                onBlocking { fetchChildren }.thenReturn({ emptyList() }, { children })
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            underTest(parent).test {
                Truth.assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Favourite))))
                Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }

        }

    @Test
    internal fun `test that new items are emitted if a non PublicLink update for an existing public node is emitted`() =
        runTest {
            val nodeId = NodeId(42)
            val children = listOf<FolderNode>(mock(), mock { on { id }.thenReturn(nodeId) })
            val parent = mock<FolderNode> {
                onBlocking { fetchChildren }.thenReturn({ children }, { emptyList() })
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            val update = NodeUpdate(
                mapOf(
                    mock<Node> {
                        on { id }.thenReturn(nodeId)
                    } to listOf(
                        NodeChanges.Favourite
                    )
                )
            )

            underTest(parent).test {
                Truth.assertThat(awaitItem()).isEqualTo(children)
                nodeUpdateChannel.send(update)
                Truth.assertThat(awaitItem()).isEmpty()
                Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @Test
    internal fun `test that only the latest ids are considered when node update is emitted`() =
        runTest {
            val nodeId = NodeId(42)
            val children = listOf<FolderNode>(mock(), mock { on { id }.thenReturn(nodeId) })
            val parent = mock<FolderNode> {
                onBlocking { fetchChildren }.thenReturn({ children }, { emptyList() })
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            val update = NodeUpdate(
                mapOf(
                    mock<Node> {
                        on { id }.thenReturn(nodeId)
                    } to listOf(
                        NodeChanges.Favourite
                    )
                )
            )

            underTest(parent).test {
                Truth.assertThat(awaitItem()).isEqualTo(children)
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
                Truth.assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(update)
                Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }
}