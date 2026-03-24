package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
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
class ShareToMegaViewModelTest {

    private lateinit var viewModel: ShareToMegaViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val shareUri = mock<UriPath>()

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase)
        getRootNodeIdUseCase.stub {
            onBlocking { invoke() } doReturn NodeId(100L)
        }
        viewModel = ShareToMegaViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            args = ShareToMegaViewModel.Args(listOf(shareUri)),
        )
    }

    @Test
    fun `test that args expose share uris`() {
        assertThat(viewModel.args.shareUris).containsExactly(shareUri)
    }

    @Test
    fun `test that ui state exposes root node id from use case`() = runTest(testDispatcher) {
        val expectedRoot = NodeId(42L)
        getRootNodeIdUseCase.stub {
            onBlocking { invoke() } doReturn expectedRoot
        }
        viewModel = ShareToMegaViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            args = ShareToMegaViewModel.Args(listOf(shareUri)),
        )

        viewModel.uiState.test {
            var state: ShareToMegaUiState = awaitItem()
            if (state is ShareToMegaUiState.Loading) {
                state = awaitItem()
            }
            val data = state as ShareToMegaUiState.Data
            assertThat(data.rootNodeId).isEqualTo(expectedRoot)
            assertThat(data.openFolderEvent).isEqualTo(consumed())
            assertThat(data.uploadEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that ui state uses fallback root id when use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn null
            }
            viewModel = ShareToMegaViewModel(
                getRootNodeIdUseCase = getRootNodeIdUseCase,
                args = ShareToMegaViewModel.Args(listOf(shareUri)),
            )

            viewModel.uiState.test {
                var state: ShareToMegaUiState = awaitItem()
                if (state is ShareToMegaUiState.Loading) {
                    state = awaitItem()
                }
                val data = state as ShareToMegaUiState.Data
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
