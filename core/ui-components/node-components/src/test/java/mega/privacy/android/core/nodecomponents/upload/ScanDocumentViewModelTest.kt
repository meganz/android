package mega.privacy.android.core.nodecomponents.upload

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanDocumentViewModelTest {
    private val scannerHandler = mock<ScannerHandler>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private lateinit var viewModel: ScanDocumentViewModel

    @AfterEach
    fun tearDown() {
        reset(scannerHandler, getFeatureFlagValueUseCase)
    }

    private fun initViewModel() {
        viewModel = ScanDocumentViewModel(
            scannerHandler = scannerHandler,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that prepareDocumentScanner triggers navigate event when feature flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(true)
            initViewModel()

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.navigateToCustomScannerEvent).isEqualTo(triggered)
                assertThat(state.gmsDocumentScanner).isNull()
            }
        }

    @Test
    fun `test that prepareDocumentScanner does not call scannerHandler when feature flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(true)
            initViewModel()

            viewModel.prepareDocumentScanner()

            verifyNoInteractions(scannerHandler)
        }

    @Test
    fun `test that prepareDocumentScanner updates state with gmsDocumentScanner on success`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(false)
            val gmsDocumentScanner = mock<GmsDocumentScanner>()
            whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)
            initViewModel()

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isEqualTo(gmsDocumentScanner)
                assertThat(state.documentScanningError).isNull()
                assertThat(state.navigateToCustomScannerEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that prepareDocumentScanner updates state with InsufficientRAM error on failure`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(false)
            whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                throw InsufficientRAMToLaunchDocumentScanner()
            }
            initViewModel()

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isNull()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)
            }
        }

    @Test
    fun `test that prepareDocumentScanner updates state with GenericError on other failure`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(false)
            whenever(scannerHandler.prepareDocumentScanner()).thenThrow(RuntimeException("Test exception"))
            initViewModel()

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isNull()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.GenericError)
            }
        }

    @Test
    fun `test that onDocumentScannerFailedToOpen updates state with GenericError`() = runTest {
        initViewModel()
        viewModel.onDocumentScannerFailedToOpen()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.GenericError)
        }
    }

    @Test
    fun `test that onGmsDocumentScannerConsumed resets gmsDocumentScanner to null`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
            .thenReturn(false)
        initViewModel()
        val gmsDocumentScanner = mock<GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)

        viewModel.prepareDocumentScanner()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.gmsDocumentScanner).isEqualTo(gmsDocumentScanner)

            viewModel.onGmsDocumentScannerConsumed()
            val updatedState = awaitItem()
            assertThat(updatedState.gmsDocumentScanner).isNull()
        }
    }

    @Test
    fun `test that onDocumentScanningErrorConsumed resets documentScanningError to null`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(false)
            initViewModel()
            whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                throw InsufficientRAMToLaunchDocumentScanner()
            }

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)

                viewModel.onDocumentScanningErrorConsumed()
                val updatedState = awaitItem()
                assertThat(updatedState.documentScanningError).isNull()
            }
        }

    @Test
    fun `test that onNavigateToCustomScannerConsumed resets event to consumed`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
                .thenReturn(true)
            initViewModel()

            viewModel.prepareDocumentScanner()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.navigateToCustomScannerEvent).isEqualTo(triggered)

                viewModel.onNavigateToCustomScannerConsumed()
                val updatedState = awaitItem()
                assertThat(updatedState.navigateToCustomScannerEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that prepareDocumentScanner handles multiple calls correctly`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.ContinuousDocumentScanner))
            .thenReturn(false)
        initViewModel()
        val gmsDocumentScanner1 = mock<GmsDocumentScanner>()
        val gmsDocumentScanner2 = mock<GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner())
            .thenReturn(gmsDocumentScanner1)
            .thenReturn(gmsDocumentScanner2)

        viewModel.prepareDocumentScanner()

        viewModel.uiState.test {
            val state1 = awaitItem()
            assertThat(state1.gmsDocumentScanner).isEqualTo(gmsDocumentScanner1)

            viewModel.onGmsDocumentScannerConsumed()
            awaitItem()

            viewModel.prepareDocumentScanner()

            val state2 = awaitItem()
            assertThat(state2.gmsDocumentScanner).isEqualTo(gmsDocumentScanner2)
        }
    }
}
