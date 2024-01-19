package mega.privacy.android.app.presentation.node

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val checkNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val moveNodesUseCase = mock<MoveNodesUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val deleteNodeVersionsUseCase = mock<DeleteNodeVersionsUseCase>()
    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun initialize() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    private fun initViewModel() {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = NodeBottomSheetActionMapper(),
            bottomSheetOptions = setOf(),
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbish = isNodeInRubbish,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            applicationScope = applicationScope,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get bottom sheet option invokes getNodeByIdUseCase`() = runTest {
        initViewModel()
        viewModel.getBottomSheetOptions(sampleNode.id.longValue)
        verify(getNodeByIdUseCase).invoke(sampleNode.id)
        verify(isNodeInRubbish).invoke(sampleNode.id.longValue)
        verify(isNodeInBackupsUseCase).invoke(sampleNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleNode.id)
    }

    @Test
    fun `test that moveRequestResults are updated properly in state`() =
        runTest {
            whenever(moveNodesUseCase(emptyMap())).thenThrow(RuntimeException())
            initViewModel()
            viewModel.moveNodes(emptyMap())
            viewModel.state.test {
                val stateOne = awaitItem()
                assertThat(stateOne.moveRequestResult).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
            viewModel.markHandleMoveRequestResult()
            viewModel.state.test {
                val stateTwo = awaitItem()
                assertThat(stateTwo.moveRequestResult).isInstanceOf(
                    StateEventWithContentConsumed::class.java
                )
            }
        }

    @Test
    fun `test that node name collision results are updated properly in state`() = runTest {
        whenever(checkNodesNameCollisionUseCase(listOf(1).associate {
            Pair(1, sampleNode.id.longValue)
        }, NodeNameCollisionType.MOVE)).thenReturn(
            NodeNameCollisionResult(
                emptyMap(),
                emptyMap(),
                NodeNameCollisionType.MOVE
            )
        )
        initViewModel()
        viewModel.checkNodesNameCollision(
            listOf(1),
            sampleNode.id.longValue,
            NodeNameCollisionType.MOVE
        )
        viewModel.state.test {
            val stateOne = awaitItem()
            assertThat(stateOne.nodeNameCollisionResult).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }
        viewModel.markHandleNodeNameCollisionResult()
        viewModel.state.test {
            val stateTwo = awaitItem()
            assertThat(stateTwo.nodeNameCollisionResult).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }
    }

    @Test
    fun `test that setMoveTargetPath is called when move node is success`() = runTest {
        whenever(moveNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        viewModel.moveNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setMoveLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that deleteNodeVersionsUseCase is triggered when delete node history is called`() =
        runTest {
            whenever(deleteNodeVersionsUseCase(sampleNode.id)).thenReturn(Unit)
            initViewModel()
            viewModel.deleteVersionHistory(sampleNode.id.longValue)
            viewModel.state.test {
                val stateOne = awaitItem()
                assertThat(stateOne.deleteVersionsResult).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
            verify(deleteNodeVersionsUseCase).invoke(sampleNode.id)
            viewModel.markHandleDeleteVersionsResult()
            viewModel.state.test {
                val stateTwo = awaitItem()
                assertThat(stateTwo.deleteVersionsResult).isInstanceOf(
                    StateEventWithContentConsumed::class.java
                )
            }
        }

}