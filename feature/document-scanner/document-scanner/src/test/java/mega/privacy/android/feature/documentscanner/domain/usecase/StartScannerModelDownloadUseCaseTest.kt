package mega.privacy.android.feature.documentscanner.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartScannerModelDownloadUseCaseTest {

    private lateinit var underTest: StartScannerModelDownloadUseCase

    private val scannerModelDownloadScheduler = mock<ScannerModelDownloadScheduler>()

    @BeforeAll
    fun setUp() {
        underTest = StartScannerModelDownloadUseCase(scannerModelDownloadScheduler)
    }

    @BeforeEach
    fun resetMocks() {
        reset(scannerModelDownloadScheduler)
    }

    @Test
    fun `test that invoke enqueues an immediate download when un-metered is not required`() =
        runTest {
            underTest(requireUnmeteredNetwork = false)

            verify(scannerModelDownloadScheduler).enqueueModelDownload(false)
        }

    @Test
    fun `test that invoke enqueues a deferred download when un-metered is required`() = runTest {
        underTest(requireUnmeteredNetwork = true)

        verify(scannerModelDownloadScheduler).enqueueModelDownload(true)
    }
}
