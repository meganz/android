package mega.privacy.android.feature.documentscanner.data.worker

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import androidx.work.WorkManager
import dagger.Lazy

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkManagerScannerModelDownloadSchedulerTest {

    private lateinit var underTest: WorkManagerScannerModelDownloadScheduler

    private val workManager = mock<WorkManager>()
    private val workManagerLazy = Lazy { workManager }

    @BeforeEach
    fun setUp() {
        reset(workManager)
        underTest = WorkManagerScannerModelDownloadScheduler(workManagerLazy)
    }

    private fun captureEnqueuedRequest(): OneTimeWorkRequest {
        val captor = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(
            eq(ScannerModelDownloadWorker.UNIQUE_NAME),
            eq(ExistingWorkPolicy.KEEP),
            captor.capture(),
        )
        return captor.firstValue
    }

    @Test
    fun `test that an immediate download is enqueued on any connected network`() = runTest {
        underTest.enqueueModelDownload(requireUnmeteredNetwork = false)

        val request = captureEnqueuedRequest()
        assertThat(request.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(request.workSpec.constraints.requiresBatteryNotLow()).isTrue()
    }

    @Test
    fun `test that a declined download is deferred to an un-metered network`() = runTest {
        underTest.enqueueModelDownload(requireUnmeteredNetwork = true)

        val request = captureEnqueuedRequest()
        assertThat(request.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.UNMETERED)
        assertThat(request.workSpec.constraints.requiresBatteryNotLow()).isTrue()
    }

    private fun stubWorkInfo(workInfo: WorkInfo?) {
        val workInfos = if (workInfo == null) emptyList() else listOf(workInfo)
        whenever(
            workManager.getWorkInfosForUniqueWorkFlow(ScannerModelDownloadWorker.UNIQUE_NAME)
        ).thenReturn(flowOf(workInfos))
    }

    private fun mockWorkInfo(
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
        outputData: Data = Data.EMPTY,
        runAttemptCount: Int = 0,
    ): WorkInfo = mock {
        on { this.state } doReturn state
        on { this.progress } doReturn progress
        on { this.outputData } doReturn outputData
        on { this.runAttemptCount } doReturn runAttemptCount
    }

    @Test
    fun `test that monitor emits NotStarted when there is no work`() = runTest {
        stubWorkInfo(null)

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.NotStarted)
    }

    @Test
    fun `test that monitor emits Pending when the work is enqueued`() = runTest {
        stubWorkInfo(mockWorkInfo(WorkInfo.State.ENQUEUED))

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Pending)
    }

    @Test
    fun `test that monitor emits Downloading with progress when the work is running`() = runTest {
        stubWorkInfo(
            mockWorkInfo(
                state = WorkInfo.State.RUNNING,
                progress = workDataOf(
                    ScannerModelDownloadWorker.KEY_BYTES_DOWNLOADED to 40L,
                    ScannerModelDownloadWorker.KEY_TOTAL_BYTES to 100L,
                ),
            )
        )

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Downloading(bytesDownloaded = 40L, totalBytes = 100L))
    }

    @Test
    fun `test that monitor emits Pending when running without a known total`() = runTest {
        stubWorkInfo(mockWorkInfo(WorkInfo.State.RUNNING))

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Pending)
    }

    @Test
    fun `test that monitor emits Retrying when enqueued after a failed attempt`() = runTest {
        stubWorkInfo(mockWorkInfo(WorkInfo.State.ENQUEUED, runAttemptCount = 1))

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Retrying)
    }

    @Test
    fun `test that monitor emits Retrying when re-running without progress after a failed attempt`() =
        runTest {
            stubWorkInfo(mockWorkInfo(WorkInfo.State.RUNNING, runAttemptCount = 2))

            assertThat(underTest.monitorModelDownload().first())
                .isEqualTo(ScannerModelDownloadState.Retrying)
        }

    @Test
    fun `test that monitor emits Completed when the work succeeds`() = runTest {
        stubWorkInfo(mockWorkInfo(WorkInfo.State.SUCCEEDED))

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Completed)
    }

    @Test
    fun `test that monitor emits permanent Failed when the failure reason is permanent`() = runTest {
        stubWorkInfo(
            mockWorkInfo(
                state = WorkInfo.State.FAILED,
                outputData = workDataOf(
                    ScannerModelDownloadWorker.KEY_FAILURE_REASON to ScannerModelDownloadWorker.FAILURE_PERMANENT,
                ),
            )
        )

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Failed(permanent = true))
    }

    @Test
    fun `test that monitor emits recoverable Failed when the failure reason is transient`() = runTest {
        stubWorkInfo(
            mockWorkInfo(
                state = WorkInfo.State.FAILED,
                outputData = workDataOf(
                    ScannerModelDownloadWorker.KEY_FAILURE_REASON to ScannerModelDownloadWorker.FAILURE_TRANSIENT,
                ),
            )
        )

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Failed(permanent = false))
    }

    @Test
    fun `test that monitor emits recoverable Failed when the work is cancelled`() = runTest {
        stubWorkInfo(mockWorkInfo(WorkInfo.State.CANCELLED))

        assertThat(underTest.monitorModelDownload().first())
            .isEqualTo(ScannerModelDownloadState.Failed(permanent = false))
    }
}
