package mega.privacy.android.domain.usecase.node.publiclink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Offline
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
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorLinksUseCaseTest {
    private lateinit var underTest: MonitorLinksUseCase

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
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()

    @BeforeAll
    internal fun setUp() {
        underTest = MonitorLinksUseCase(
            shareRepository = shareRepository,
            mapNodeToPublicLinkUseCase = mapNodeToPublicLinkUseCase,
            nodeRepository = nodeRepository,
            getLinksSortOrderUseCase = getLinksSortOrderUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            shareRepository,
            mapNodeToPublicLinkUseCase,
            nodeRepository,
            getLinksSortOrderUseCase,
            monitorOfflineNodeUpdatesUseCase
        )
        nodeRepository.stub {
            on { monitorNodeUpdates() }.thenReturn(flow { awaitCancellation() })
        }
        mapNodeToPublicLinkUseCase.stub {
            onBlocking { invoke(any(), anyOrNull()) }.thenReturn(mock<PublicLinkFolder>())
        }
        getLinksSortOrderUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that folder link nodes are returned`(isSingleActivityEnabled: Boolean) =
        runTest {
            val untypedNodes = listOf<FolderNode>(mock(), mock())
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(untypedNodes)
            }
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

            underTest(isSingleActivityEnabled).test {
                assertThat(awaitItem()).hasSize(untypedNodes.size)
                cancelAndConsumeRemainingEvents()
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that new items are emitted if a node update of type PublicLink is emitted`(
        isSingleActivityEnabled: Boolean,
    ) = runTest {
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
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

        underTest(isSingleActivityEnabled).test {
            assertThat(awaitItem()).isEmpty()
            nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
            assertThat(awaitItem()).hasSize(untypedNodes.size)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that no new items are emitted if a node update of type not of PublicLink is emitted`(
        isSingleActivityEnabled: Boolean,
    ) = runTest {
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
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

        underTest(isSingleActivityEnabled).test {
            assertThat(awaitItem()).isEmpty()
            nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Favourite))))
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that new items are emitted if a non PublicLink update for an existing public node is emitted`(
        isSingleActivityEnabled: Boolean,
    ) = runTest {
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
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

        val update = NodeUpdate(
            mapOf(
                mock<Node> {
                    on { id }.thenReturn(nodeId)
                } to listOf(NodeChanges.Favourite)
            )
        )

        underTest(isSingleActivityEnabled).test {
            assertThat(awaitItem()).hasSize(untypedNodes.size)
            nodeUpdateChannel.send(update)
            assertThat(awaitItem()).isEmpty()
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    internal fun `test that public links are refreshed when offline node matching public link is added`() =
        runTest {
            val nodeId = NodeId(123L)
            val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    untypedNodes,
                    untypedNodes
                )
            }
            val offlineNode = Offline(
                id = 1,
                handle = "123",
                path = "/test",
                name = "test",
                parentId = 0,
                type = Offline.FILE,
                origin = Offline.OTHER,
                handleIncoming = ""
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flowOf(
                    emptyList(), // First emission (dropped)
                    emptyList(), // Second emission (scan initial state)
                    listOf(offlineNode) // Third emission (should trigger refresh)
                )
            )

            underTest(true).test {
                assertThat(awaitItem()).hasSize(1)
                assertThat(awaitItem()).hasSize(1)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that public links are refreshed when offline node matching public link is removed`() =
        runTest {
            val nodeId = NodeId(123L)
            val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(
                    untypedNodes,
                    untypedNodes
                )
            }
            val offlineNode = Offline(
                id = 1,
                handle = "123",
                path = "/test",
                name = "test",
                parentId = 0,
                type = Offline.FILE,
                origin = Offline.OTHER,
                handleIncoming = ""
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flowOf(
                    emptyList(), // First emission (dropped)
                    emptyList(), // Second emission (scan initial state)
                    listOf(offlineNode), // Third emission (node added)
                    emptyList() // Fourth emission (node removed - should trigger refresh)
                )
            )

            underTest(true).test {
                assertThat(awaitItem()).hasSize(1)
                assertThat(awaitItem()).hasSize(1) // After offline node added
                assertThat(awaitItem()).hasSize(1) // After offline node removed
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that public links are not refreshed when offline node does not match any public link`() =
        runTest {
            val nodeId = NodeId(123L)
            val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(untypedNodes)
            }
            val nonMatchingOfflineNode = Offline(
                id = 1,
                handle = "999",
                path = "/test",
                name = "test",
                parentId = 0,
                type = Offline.FILE,
                origin = Offline.OTHER,
                handleIncoming = ""
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flowOf(
                    emptyList(), // First emission (dropped)
                    emptyList(), // Second emission (scan initial state)
                    listOf(nonMatchingOfflineNode) // Third emission (should not trigger refresh)
                )
            )

            underTest(true).test {
                assertThat(awaitItem()).hasSize(1)
                // Should not emit again because offline node doesn't match
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @Test
    internal fun `test that offline node updates are skipped when no public links exist`() =
        runTest {
            shareRepository.stub {
                onBlocking { getPublicLinks(any()) }.thenReturn(emptyList())
            }
            val offlineNode = Offline(
                id = 1,
                handle = "123",
                path = "/test",
                name = "test",
                parentId = 0,
                type = Offline.FILE,
                origin = Offline.OTHER,
                handleIncoming = ""
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flowOf(
                    emptyList(), // First emission (dropped)
                    emptyList(), // Second emission (scan initial state)
                    listOf(offlineNode) // Third emission (should be filtered out)
                )
            )

            underTest(true).test {
                assertThat(awaitItem()).isEmpty()
                // Should not emit again because nodeIds is empty
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @Test
    internal fun `test that first offline emission is dropped`() = runTest {
        val nodeId = NodeId(123L)
        val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
        shareRepository.stub {
            onBlocking { getPublicLinks(any()) }.thenReturn(untypedNodes)
        }
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "/test",
            name = "test",
            parentId = 0,
            type = Offline.FILE,
            origin = Offline.OTHER,
            handleIncoming = ""
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flowOf(
                listOf(offlineNode), // First emission (should be dropped)
                emptyList(), // Second emission (scan initial state)
                listOf(offlineNode) // Third emission (should trigger refresh)
            )
        )

        underTest(true).test {
            assertThat(awaitItem()).hasSize(1)
            assertThat(awaitItem()).hasSize(1)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    internal fun `test that distinct offline updates are handled correctly`() = runTest {
        val nodeId = NodeId(123L)
        val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
        shareRepository.stub {
            onBlocking { getPublicLinks(any()) }.thenReturn(untypedNodes)
        }
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "/test",
            name = "test",
            parentId = 0,
            type = Offline.FILE,
            origin = Offline.OTHER,
            handleIncoming = ""
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flowOf(
                emptyList(), // First emission (dropped)
                emptyList(), // Second emission (scan initial state)
                listOf(offlineNode), // Third emission (should trigger)
                listOf(offlineNode) // Fourth emission (same set, should not trigger due to distinctUntilChanged)
            )
        )

        underTest(true).test {
            assertThat(awaitItem()).hasSize(1)
            assertThat(awaitItem()).hasSize(1)
            // Should not emit again because the set hasn't changed
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    internal fun `test that online and offline updates are merged correctly`() = runTest {
        val nodeId = NodeId(123L)
        val untypedNodes = listOf<FolderNode>(mock { on { id }.thenReturn(nodeId) })
        shareRepository.stub {
            onBlocking { getPublicLinks(any()) }.thenReturn(
                untypedNodes,
                untypedNodes,
                untypedNodes
            )
        }
        val nodeUpdateChannel = Channel<NodeUpdate>()
        nodeRepository.stub {
            on { monitorNodeUpdates() }.thenReturn(nodeUpdateChannel.consumeAsFlow())
        }
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "/test",
            name = "test",
            parentId = 0,
            type = Offline.FILE,
            origin = Offline.OTHER,
            handleIncoming = ""
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flowOf(
                emptyList(), // First emission (dropped)
                emptyList(), // Second emission (scan initial state)
                listOf(offlineNode) // Third emission (offline update)
            )
        )

        underTest(true).test {
            assertThat(awaitItem()).hasSize(1)
            // Send online update
            nodeUpdateChannel.send(NodeUpdate(mapOf(mock<Node>() to listOf(NodeChanges.Public_link))))
            assertThat(awaitItem()).hasSize(1)
            // Offline update should also trigger
            assertThat(awaitItem()).hasSize(1)
            cancelAndConsumeRemainingEvents()
        }
    }
}

