package mega.privacy.android.feature.cloudexplorer.presentation.addvideotoplaylist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
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
class AddVideoToPlaylistViewModelTest {

    private lateinit var viewModel: AddVideoToPlaylistViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase)
        getRootNodeIdUseCase.stub {
            onBlocking { invoke() } doReturn NodeId(100L)
        }
        viewModel = AddVideoToPlaylistViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
        )
    }

    @Test
    fun `test that ui state exposes root node id from use case`() = runTest(testDispatcher) {
        val expectedRoot = NodeId(42L)
        getRootNodeIdUseCase.stub {
            onBlocking { invoke() } doReturn expectedRoot
        }
        viewModel = AddVideoToPlaylistViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
        )

        viewModel.uiState.test {
            var state: AddVideoToPlaylistUiState = awaitItem()
            if (state is AddVideoToPlaylistUiState.Loading) {
                state = awaitItem()
            }
            val data = state as AddVideoToPlaylistUiState.Data
            assertThat(data.rootNodeId).isEqualTo(expectedRoot)
        }
    }

    @Test
    fun `test that ui state uses fallback root id when use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn null
            }
            viewModel = AddVideoToPlaylistViewModel(
                getRootNodeIdUseCase = getRootNodeIdUseCase,
            )

            viewModel.uiState.test {
                var state: AddVideoToPlaylistUiState = awaitItem()
                if (state is AddVideoToPlaylistUiState.Loading) {
                    state = awaitItem()
                }
                val data = state as AddVideoToPlaylistUiState.Data
                assertThat(data.rootNodeId).isEqualTo(NodeId(-1))
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
