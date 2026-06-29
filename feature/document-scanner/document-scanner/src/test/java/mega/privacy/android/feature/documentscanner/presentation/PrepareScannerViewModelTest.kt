package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.domain.usecase.MonitorScannerModelDownloadUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.StartScannerModelDownloadUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class PrepareScannerViewModelTest {

    private lateinit var underTest: PrepareScannerViewModel

    private val monitorScannerModelDownload = mock<MonitorScannerModelDownloadUseCase>()
    private val startScannerModelDownload = mock<StartScannerModelDownloadUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(monitorScannerModelDownload, startScannerModelDownload)
    }

    private fun initTestSubject() {
        underTest = PrepareScannerViewModel(monitorScannerModelDownload, startScannerModelDownload)
    }

    @Test
    fun `test that uiState starts in the Pending download state`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(flowOf(ScannerModelDownloadState.Pending))

        initTestSubject()

        underTest.uiState.test {
            assertThat(awaitItem().downloadState).isEqualTo(ScannerModelDownloadState.Pending)
        }
    }

    @Test
    fun `test that uiState reflects the download progress reported by the monitor use case`() =
        runTest {
            whenever(monitorScannerModelDownload()).thenReturn(
                flowOf(
                    ScannerModelDownloadState.Downloading(bytesDownloaded = 50L, totalBytes = 100L),
                )
            )

            initTestSubject()

            underTest.uiState.test {
                assertThat(awaitItem().downloadState)
                    .isEqualTo(ScannerModelDownloadState.Downloading(bytesDownloaded = 50L, totalBytes = 100L))
            }
        }

    @Test
    fun `test that uiState reflects a failed download`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(
            flowOf(ScannerModelDownloadState.Failed(permanent = true))
        )

        initTestSubject()

        underTest.uiState.test {
            assertThat(awaitItem().downloadState)
                .isEqualTo(ScannerModelDownloadState.Failed(permanent = true))
        }
    }

    @Test
    fun `test that onRetryDownload re-enqueues an immediate download`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(flowOf(ScannerModelDownloadState.Pending))

        initTestSubject()
        underTest.onRetryDownload()

        verify(startScannerModelDownload).invoke(requireUnmeteredNetwork = false)
    }

    @Test
    fun `test that onRetryDownload swallows failures from the download use case`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(flowOf(ScannerModelDownloadState.Pending))
        whenever(startScannerModelDownload(requireUnmeteredNetwork = false))
            .thenAnswer { throw IllegalStateException("boom") }

        initTestSubject()
        underTest.onRetryDownload()

        verify(startScannerModelDownload).invoke(requireUnmeteredNetwork = false)
    }

    @Test
    fun `test that modelReadyEvent is triggered when download state is Completed`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(
            flowOf(ScannerModelDownloadState.Completed)
        )

        initTestSubject()

        underTest.uiState.test {
            assertThat(awaitItem().modelReadyEvent).isEqualTo(triggered)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that modelReadyEvent is consumed after onModelReadyConsumed is called`() = runTest {
        whenever(monitorScannerModelDownload()).thenReturn(
            flowOf(ScannerModelDownloadState.Completed)
        )

        initTestSubject()

        underTest.uiState.test {
            awaitItem() // (Completed, triggered) — upstream runs eagerly before Turbine polls
            underTest.onModelReadyConsumed()
            assertThat(awaitItem().modelReadyEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the monitor use case is not invoked before the state is collected`() = runTest {
        initTestSubject()

        verifyNoInteractions(monitorScannerModelDownload)
    }
}
