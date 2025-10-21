package mega.privacy.android.domain.usecase.node.publiclink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorPublicLinksUseCaseTest {
    private lateinit var underTest: MonitorPublicLinksUseCase

    private val shareRepository = mock<ShareRepository>()

    private val mapNodeToPublicLinkUseCase = mock<MapNodeToPublicLinkUseCase> {
        onBlocking { invoke(any(), anyOrNull()) }.thenReturn(mock<PublicLinkFolder>())
    }

    private val nodeRepository = mock<NodeRepository> {
        on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
    }

    private val getLinksSortOrderUseCase = mock<GetLinksSortOrderUseCase>() {
        onBlocking { invoke(any()) }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }

    @BeforeAll
    internal fun setUp() {
        underTest = MonitorPublicLinksUseCase(
            shareRepository = shareRepository,
            mapNodeToPublicLinkUseCase = mapNodeToPublicLinkUseCase,
            nodeRepository = nodeRepository,
            getLinksSortOrderUseCase = getLinksSortOrderUseCase
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that folder link nodes are returned`(isSingleActivityEnabled: Boolean) =
        runTest {
            val untypedNodes = listOf<FolderNode>(mock(), mock())
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    untypedNodes
                )
            }

            underTest(isSingleActivityEnabled).test {
                assertThat(awaitItem()).hasSize(untypedNodes.size)
                cancelAndConsumeRemainingEvents()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that new items are emitted if a node update of type PublicLink is emitted`(isSingleActivityEnabled: Boolean) =
        runTest {
            val untypedNodes = listOf<FolderNode>(mock(), mock())
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    emptyList(),
                    untypedNodes
                )
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            underTest(isSingleActivityEnabled).test {
                assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
                assertThat(awaitItem()).hasSize(untypedNodes.size)
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }

        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that no new items are emitted if a node update of type not of PublicLink is emitted`(isSingleActivityEnabled: Boolean) =
        runTest {
            val untypedNodes = listOf<FolderNode>(mock(), mock())
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    emptyList(),
                    untypedNodes
                )
            }
            val nodeUpdateChannel = Channel<NodeUpdate>()
            nodeRepository.stub {
                on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
            }

            underTest(false).test {
                assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Favourite))))
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }

        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that new items are emitted if a non PublicLink update for an existing public node is emitted`(isSingleActivityEnabled: Boolean) =
        runTest {
            val nodeId = NodeId(42)
            val untypedNodes = listOf<FolderNode>(mock(), mock { on { id }.thenReturn(nodeId) })
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    untypedNodes,
                    emptyList(),
                )
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

            underTest(isSingleActivityEnabled).test {
                assertThat(awaitItem()).hasSize(untypedNodes.size)
                nodeUpdateChannel.send(update)
                assertThat(awaitItem()).isEmpty()
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that only the latest ids are considered when node update is emitted`(isSingleActivityEnabled: Boolean) =
        runTest {
            val nodeId = NodeId(42)
            val untypedNodes = listOf<FolderNode>(mock(), mock { on { id }.thenReturn(nodeId) })
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    untypedNodes,
                    emptyList(),
                )
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

            underTest(isSingleActivityEnabled).test {
                assertThat(awaitItem()).hasSize(untypedNodes.size)
                nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
                assertThat(awaitItem()).isEmpty()
                nodeUpdateChannel.send(update)
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }
}