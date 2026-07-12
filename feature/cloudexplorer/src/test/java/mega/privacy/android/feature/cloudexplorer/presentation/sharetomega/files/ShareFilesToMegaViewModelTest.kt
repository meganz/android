package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFilesToMegaViewModelTest {

    private lateinit var viewModel: ShareFilesToMegaViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val filePrepareUseCase = mock<FilePrepareUseCase>()
    private val shareUri = UriPath("content://test/uri")

    private fun createViewModel(shareUris: List<UriPath> = listOf(shareUri)) =
        ShareFilesToMegaViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            filePrepareUseCase = filePrepareUseCase,
            args = ShareFilesToMegaViewModel.Args(shareUris),
        )

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase, filePrepareUseCase)
        getRootNodeIdUseCase.stub {
            onBlocking { invoke() } doReturn NodeId(100L)
        }
        filePrepareUseCase.stub {
            onBlocking { invoke(any()) } doReturn listOf(mock<DocumentEntity>())
        }
        viewModel = createViewModel()
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
        viewModel = createViewModel()

        viewModel.uiState.test {
            var state: ShareFilesToMegaUiState = awaitItem()
            if (state is ShareFilesToMegaUiState.Loading) {
                state = awaitItem()
            }
            val data = state as ShareFilesToMegaUiState.Data
            assertThat(data.rootNodeId).isEqualTo(expectedRoot)
        }
    }

    @Test
    fun `test that ui state uses fallback root id when use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn null
            }
            viewModel = createViewModel()

            viewModel.uiState.test {
                var state: ShareFilesToMegaUiState = awaitItem()
                if (state is ShareFilesToMegaUiState.Loading) {
                    state = awaitItem()
                }
                val data = state as ShareFilesToMegaUiState.Data
                assertThat(data.rootNodeId).isEqualTo(NodeId(-1))
            }
        }

    @Test
    fun `test that ui state flags hasNoFilesToUpload when the shared uris resolve to no files`() =
        runTest(testDispatcher) {
            filePrepareUseCase.stub {
                onBlocking { invoke(any()) } doReturn emptyList()
            }
            viewModel = createViewModel()

            viewModel.uiState.test {
                var state = awaitItem()
                while (state !is ShareFilesToMegaUiState.Data || !state.hasNoFilesToUpload) {
                    state = awaitItem()
                }
                assertThat((state as ShareFilesToMegaUiState.Data).hasNoFilesToUpload).isTrue()
            }
        }

    @Test
    fun `test that ui state keeps hasNoFilesToUpload false when the shared uris resolve to files`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            viewModel.uiState.test {
                var state: ShareFilesToMegaUiState = awaitItem()
                if (state is ShareFilesToMegaUiState.Loading) {
                    state = awaitItem()
                }
                val data = state as ShareFilesToMegaUiState.Data
                assertThat(data.hasNoFilesToUpload).isFalse()
                expectNoEvents()
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
