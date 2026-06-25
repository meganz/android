package mega.privacy.android.feature.documentscanner.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.argumentCaptor
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
}
