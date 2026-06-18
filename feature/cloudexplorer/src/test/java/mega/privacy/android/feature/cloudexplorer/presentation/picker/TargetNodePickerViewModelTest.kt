package mega.privacy.android.feature.cloudexplorer.presentation.picker

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
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TargetNodePickerViewModelTest {

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase, getNodeNavigationStackUseCase)
    }

    @Test
    fun `test that the target path is empty when there is no latest target`() =
        runTest(testDispatcher) {
            stubRoot(ROOT)
            val underTest = viewModel(latestTarget = null)

            underTest.uiState.test {
                val data = awaitData()
                assertThat(data.rootNodeId).isEqualTo(ROOT)
                assertThat(data.targetPath).isEmpty()
                assertThat(data.nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
            }
        }

    @Test
    fun `test that the target path is empty when the latest target is the root`() =
        runTest(testDispatcher) {
            stubRoot(ROOT)
            val underTest = viewModel(latestTarget = ROOT.longValue)

            underTest.uiState.test {
                assertThat(awaitData().targetPath).isEmpty()
            }
        }

    @Test
    fun `test that a target under the root resumes on cloud drive with its navigation stack`() =
        runTest(testDispatcher) {
            stubRoot(ROOT)
            val path = listOf(NodeId(5L), NodeId(6L))
            getNodeNavigationStackUseCase.stub {
                onBlocking { invoke(NodeId(TARGET)) } doReturn
                        NodeNavigationStack(stack = path, isUnderRootNode = true)
            }
            val underTest = viewModel(latestTarget = TARGET)

            underTest.uiState.test {
                val data = awaitData()
                assertThat(data.targetPath).isEqualTo(path)
                assertThat(data.nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
            }
        }

    @Test
    fun `test that a target outside the root resumes on incoming shares`() =
        runTest(testDispatcher) {
            stubRoot(ROOT)
            val path = listOf(NodeId(9L))
            getNodeNavigationStackUseCase.stub {
                onBlocking { invoke(NodeId(TARGET)) } doReturn
                        NodeNavigationStack(stack = path, isUnderRootNode = false)
            }
            val underTest = viewModel(latestTarget = TARGET)

            underTest.uiState.test {
                val data = awaitData()
                assertThat(data.targetPath).isEqualTo(path)
                assertThat(data.nodeSourceType).isEqualTo(NodeSourceType.INCOMING_SHARES)
            }
        }

    private fun stubRoot(rootNodeId: NodeId) = getRootNodeIdUseCase.stub {
        onBlocking { invoke() } doReturn rootNodeId
    }

    private fun viewModel(latestTarget: Long?) = object : TargetNodePickerViewModel(
        getRootNodeIdUseCase,
        getNodeNavigationStackUseCase,
    ) {
        override suspend fun getLatestTargetPath() = latestTarget
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<TargetNodePickerUiState>.awaitData(): TargetNodePickerUiState.Data {
        var state: TargetNodePickerUiState = awaitItem()
        if (state is TargetNodePickerUiState.Loading) state = awaitItem()
        return state as TargetNodePickerUiState.Data
    }

    companion object {
        @JvmField
        val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)

        private val ROOT = NodeId(1L)
        private const val TARGET = 42L
    }
}
