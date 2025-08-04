package mega.privacy.android.domain.usecase.node

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorNodeUpdatesByIdUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase = mock()

    private lateinit var underTest: MonitorNodeUpdatesByIdUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorNodeUpdatesByIdUseCase(
            nodeRepository = nodeRepository,
            getRootNodeUseCase = getRootNodeUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getRootNodeUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorRefreshSessionUseCase
        )
    }

    @Test
    fun `test that invoke returns NodeChanges_Attributes when folder node is not in rubbish bin`() =
        runTest {
            val nodeId = NodeId(1L)
            val folderNode = mock<FolderNode> {
                on { id } doReturn nodeId
                on { parentId } doReturn NodeId(0L)
                on { isInRubbishBin } doReturn false
            }

            val nodeUpdateFlow = flow {
                emit(
                    NodeUpdate(
                        mapOf(folderNode to listOf(NodeChanges.Attributes))
                    )
                )
            }

            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))
            whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

            underTest(nodeId).test {
                assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that invoke returns NodeChanges_Attributes for offline node updates`() = runTest {
        val nodeId = NodeId(1L)
        val offlineNode = Offline(
            id = 1,
            handle = "1",
            path = "/test",
            name = "test",
            parentId = 1,
            type = Offline.FOLDER,
            origin = Offline.OTHER,
            handleIncoming = ""
        )

        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(listOf(offlineNode)))
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(nodeId).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that invoke returns NodeChanges_Attributes for refresh session updates`() = runTest {
        val nodeId = NodeId(1L)

        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(nodeId).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that invoke uses root node id when nodeId is -1L`() = runTest {
        val rootNodeId = NodeId(0L)
        val rootNode = mock<FolderNode> {
            on { id } doReturn rootNodeId
            on { parentId } doReturn NodeId(-1L)
            on { isInRubbishBin } doReturn false
        }

        val nodeUpdateFlow = flow {
            emit(
                NodeUpdate(
                    mapOf(rootNode to listOf(NodeChanges.Attributes))
                )
            )
        }

        whenever(getRootNodeUseCase()).thenReturn(rootNode)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(NodeId(-1L)).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that invoke falls back to original nodeId when root node is null`() = runTest {
        val nodeId = NodeId(-1L)
        val fallbackNodeId = NodeId(-1L)
        val folderNode = mock<FolderNode> {
            on { id } doReturn fallbackNodeId
            on { parentId } doReturn NodeId(0L)
            on { isInRubbishBin } doReturn false
        }

        val nodeUpdateFlow = flow {
            emit(
                NodeUpdate(
                    mapOf(folderNode to listOf(NodeChanges.Attributes))
                )
            )
        }

        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(nodeId).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that invoke filters offline nodes by parent ID`() = runTest {
        val nodeId = NodeId(1L)
        val matchingOfflineNode = Offline(
            id = 1,
            handle = "1",
            path = "/test",
            name = "test",
            parentId = 1,
            type = Offline.FOLDER,
            origin = Offline.OTHER,
            handleIncoming = ""
        )
        val nonMatchingOfflineNode = Offline(
            id = 2,
            handle = "2",
            path = "/test2",
            name = "test2",
            parentId = 999,
            type = Offline.FOLDER,
            origin = Offline.OTHER,
            handleIncoming = ""
        )

        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flowOf(
                listOf(
                    matchingOfflineNode,
                    nonMatchingOfflineNode
                )
            )
        )
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(nodeId).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that invoke filters offline nodes by handle`() = runTest {
        val nodeId = NodeId(1L)
        val matchingOfflineNode = Offline(
            id = 1,
            handle = "1",
            path = "/test",
            name = "test",
            parentId = 999,
            type = Offline.FOLDER,
            origin = Offline.OTHER,
            handleIncoming = ""
        )

        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(listOf(matchingOfflineNode)))
        whenever(monitorRefreshSessionUseCase()).thenReturn(flowOf(Unit))

        underTest(nodeId).test {
            assertThat(awaitItem()).isEqualTo(NodeChanges.Attributes)
            cancelAndConsumeRemainingEvents()
        }
    }
}