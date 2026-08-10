package mega.privacy.android.feature.documentscanner.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorScannerModelDownloadUseCaseTest {

    private lateinit var underTest: MonitorScannerModelDownloadUseCase

    private val scannerModelDownloadScheduler = mock<ScannerModelDownloadScheduler>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorScannerModelDownloadUseCase(scannerModelDownloadScheduler)
    }

    @BeforeEach
    fun resetMocks() {
        reset(scannerModelDownloadScheduler)
    }

    @Test
    fun `test that invoke emits the states reported by the scheduler`() = runTest {
        whenever(scannerModelDownloadScheduler.monitorModelDownload()).thenReturn(
            flowOf(
                ScannerModelDownloadState.Downloading(bytesDownloaded = 10L, totalBytes = 100L),
                ScannerModelDownloadState.Completed,
            )
        )

        underTest().test {
            assertThat(awaitItem())
                .isEqualTo(ScannerModelDownloadState.Downloading(bytesDownloaded = 10L, totalBytes = 100L))
            assertThat(awaitItem()).isEqualTo(ScannerModelDownloadState.Completed)
            awaitComplete()
        }
    }
}
