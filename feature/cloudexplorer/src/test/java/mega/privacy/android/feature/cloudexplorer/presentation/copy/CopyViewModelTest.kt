package mega.privacy.android.feature.cloudexplorer.presentation.copy

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
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
internal class CopyViewModelTest {

    private lateinit var underTest: CopyViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val getCopyLatestTargetPathUseCase = mock<GetCopyLatestTargetPathUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getRootNodeIdUseCase,
            getCopyLatestTargetPathUseCase,
            getNodeByIdUseCase,
            getAncestorsIdsUseCase,
        )
    }

    private fun initUnderTest() {
        underTest = CopyViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getCopyLatestTargetPathUseCase = getCopyLatestTargetPathUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
        )
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<CopyUiState>.awaitData(): CopyUiState.Data {
        var state: CopyUiState = awaitItem()
        if (state is CopyUiState.Loading) state = awaitItem()
        return state as CopyUiState.Data
    }

    @Test
    fun `test that uiState emits Data with root and empty path when there is no last target`() =
        runTest(testDispatcher) {
            val root = NodeId(1L)
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn null }
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
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn null }
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
            val targetNode = mock<TypedNode>()
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeByIdUseCase.stub { onBlocking { invoke(target) } doReturn targetNode }
            getAncestorsIdsUseCase.stub {
                onBlocking { invoke(targetNode) } doReturn listOf(parent, root)
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
            val targetNode = mock<TypedNode>()
            getRootNodeIdUseCase.stub { onBlocking { invoke() } doReturn root }
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeByIdUseCase.stub { onBlocking { invoke(target) } doReturn targetNode }
            getAncestorsIdsUseCase.stub {
                onBlocking { invoke(targetNode) } doReturn listOf(shareRoot)
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
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn target.longValue }
            getNodeByIdUseCase.stub { onBlocking { invoke(any()) } doReturn null }
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
            getCopyLatestTargetPathUseCase.stub { onBlocking { invoke() } doReturn root.longValue }
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
            getCopyLatestTargetPathUseCase.stub {
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
