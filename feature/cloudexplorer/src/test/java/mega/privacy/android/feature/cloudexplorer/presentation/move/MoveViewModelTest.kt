package mega.privacy.android.feature.cloudexplorer.presentation.move

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNavigationStack
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.picker.TargetNodePickerUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoveViewModelTest {

    private lateinit var underTest: MoveViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val getMoveLatestTargetUseCase = mock<GetMoveLatestTargetUseCase>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getRootNodeIdUseCase,
            getMoveLatestTargetUseCase,
            getNodeNavigationStackUseCase,
        )
    }

    private fun initUnderTest() {
        underTest = MoveViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getMoveLatestTargetUseCase = getMoveLatestTargetUseCase,
            getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
        )
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<TargetNodePickerUiState>.awaitData(): TargetNodePickerUiState.Data {
        var state: TargetNodePickerUiState = awaitItem()
        if (state is TargetNodePickerUiState.Loading) state = awaitItem()
        return state as TargetNodePickerUiState.Data
    }

    @Test
    fun `test that uiState emits Data with root and empty path when there is no last target`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn null }
            initUnderTest()

            underTest.uiState.test {
                val state = awaitData()
                assertThat(state.rootNodeId).isEqualTo(root)
                assertThat(state.targetPath).isEmpty()
            }
        }

    @Test
    fun `test that uiState falls back to NodeId minus one when root use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn null }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn null }
            initUnderTest()

            underTest.uiState.test {
                val state = awaitData()
                assertThat(state.rootNodeId).isEqualTo(NodeId(-1))
                assertThat(state.targetPath).isEmpty()
            }
        }

    @Test
    fun `test that uiState resolves the target path when the last target lives under root`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            val parent = NodeId(3L)
            val target = NodeId(4L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeNavigationStackUseCase.stub {
                onBlocking { invoke(target) } doReturn NodeNavigationStack(
                    stack = listOf(parent, target),
                    isUnderRootNode = true,
                )
            }
            initUnderTest()

            underTest.uiState.test {
                val state = awaitData()
                assertThat(state.targetPath).containsExactly(parent, target).inOrder()
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
            }
        }

    @Test
    fun `test that uiState resolves an incoming-shares path when the target is not under root`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            val shareRoot = NodeId(99L)
            val target = NodeId(4L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeNavigationStackUseCase.stub {
                onBlocking { invoke(target) } doReturn NodeNavigationStack(
                    stack = listOf(shareRoot, target),
                    isUnderRootNode = false,
                )
            }
            initUnderTest()

            underTest.uiState.test {
                val state = awaitData()
                assertThat(state.targetPath).containsExactly(shareRoot, target).inOrder()
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.INCOMING_SHARES)
            }
        }

    @Test
    fun `test that uiState returns empty path when the target node can not be fetched`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            val target = NodeId(4L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeNavigationStackUseCase.stub { onBlocking { invoke(any()) } doReturn NodeNavigationStack() }
            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitData().targetPath).isEmpty()
            }
        }

    @Test
    fun `test that uiState returns empty path when the last target equals root`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub { onBlocking { invoke() } doReturn root.longValue }
            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitData().targetPath).isEmpty()
            }
        }

    @Test
    fun `test that uiState resolves root even when the last target use case throws`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getMoveLatestTargetUseCase.stub {
                onBlocking { invoke() } doAnswer { throw RuntimeException("boom") }
            }
            initUnderTest()

            underTest.uiState.test {
                val state = awaitData()
                assertThat(state.rootNodeId).isEqualTo(root)
                assertThat(state.targetPath).isEmpty()
            }
        }

    companion object {
        @JvmField
        val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
