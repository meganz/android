package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.transfers.completed.ClearCompletedTransfersCacheUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearCompletedTransfersCacheInitializerTest {

    private lateinit var underTest: ClearCompletedTransfersCacheInitializer

    private val clearCompletedTransfersCacheUseCase = mock<ClearCompletedTransfersCacheUseCase>()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()

    @BeforeAll
    fun setUp() {
        val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
            on { invoke() }.thenReturn(monitorNodeUpdatesFakeFlow)
        }
        underTest = ClearCompletedTransfersCacheInitializer(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            clearCompletedTransfersCacheUseCase = clearCompletedTransfersCacheUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(clearCompletedTransfersCacheUseCase)
    }

    @Test
    fun `test that clear completed transfers cache use case is invoked when folder node has parent change`() =
        runTest {
            val folderNode = mock<FolderNode>()
            val nodeUpdate = NodeUpdate(mapOf(folderNode to listOf(NodeChanges.Parent)))

            val job = launch {
                underTest("session", false)
            }
            advanceUntilIdle()
            monitorNodeUpdatesFakeFlow.emit(nodeUpdate)

            verify(clearCompletedTransfersCacheUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that clear completed transfers cache use case is not invoked when node update has no folder with parent change`() =
        runTest {
            val node = mock<Node>()
            val nodeUpdate = NodeUpdate(mapOf(node to listOf(NodeChanges.Name)))

            val job = launch {
                underTest("session", false)
            }
            advanceUntilIdle()
            monitorNodeUpdatesFakeFlow.emit(nodeUpdate)

            verifyNoInteractions(clearCompletedTransfersCacheUseCase)
            job.cancel()
        }

    @Test
    fun `test that clear completed transfers cache use case is not invoked when folder node has other changes but not parent`() =
        runTest {
            val folderNode = mock<FolderNode>()
            val nodeUpdate = NodeUpdate(mapOf(folderNode to listOf(NodeChanges.Name)))

            val job = launch {
                underTest("session", false)
            }
            advanceUntilIdle()
            monitorNodeUpdatesFakeFlow.emit(nodeUpdate)

            verifyNoInteractions(clearCompletedTransfersCacheUseCase)
            job.cancel()
        }
}
