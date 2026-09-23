package mega.privacy.android.app.facade

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.facade.WorkManagerGatewayImpl
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.WorkerClassGateway
import mega.privacy.android.domain.monitoring.CrashReporter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever


@RunWith(AndroidJUnit4::class)
class WorkManagerGatewayImplTest {

    private lateinit var underTest: WorkManagerGateway

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private var crashReporter = mock<CrashReporter>()
    private var workerClassGateway = mock<WorkerClassGateway>()

    private var fakeWorker = FakeWorker::class.java

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        workManager = WorkManager.getInstance(context)

        underTest = WorkManagerGatewayImpl(
            workManager = { workManager },
            crashReporter = crashReporter,
            workerClassGateway = workerClassGateway,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that delete oldest completed transfers worker is enqueued only once when calling enqueueDeleteOldestCompletedTransfersWorkRequest`() =
        runTest {
            val tag = "DELETE_OLDEST_TRANSFERS_WORKER_TAG"
            whenever(workerClassGateway.deleteOldestCompletedTransferWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueDeleteOldestCompletedTransfersWorkRequest()
            // second call to make sure that worker is enqueued only once
            underTest.enqueueDeleteOldestCompletedTransfersWorkRequest()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED) // has an initial delay
            }
        }

    @Test
    fun `test that downloads worker is enqueued only once when calling enqueueDownloadsWorkerRequest`() =
        runTest {
            val tag = "MEGA_DOWNLOAD_TAG"
            whenever(workerClassGateway.downloadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueDownloadsWorkerRequest()
            // second call to make sure that worker is enqueued only once
            underTest.enqueueDownloadsWorkerRequest()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that chat uploads worker is enqueued only once when calling enqueueChatUploadsWorkerRequest`() =
        runTest {
            val tag = "MEGA_CHAT_UPLOAD_TAG"
            whenever(workerClassGateway.chatUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueChatUploadsWorkerRequest()
            // second call to make sure that worker is enqueued only once
            underTest.enqueueChatUploadsWorkerRequest()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that download worker status info is monitored with monitorDownloadsStatusInfo`() =
        runTest {
            whenever(workerClassGateway.downloadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueDownloadsWorkerRequest()

            underTest.monitorDownloadsStatusInfo().test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that chat uploads worker status info is monitored with monitorChatUploadsStatusInfo`() =
        runTest {
            whenever(workerClassGateway.chatUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueChatUploadsWorkerRequest()

            underTest.monitorChatUploadsStatusInfo().test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that uploads worker is enqueued only once when calling enqueueUploadsWorkerRequest`() =
        runTest {
            val tag = "MEGA_UPLOAD_TAG"
            whenever(workerClassGateway.uploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueUploadsWorkerRequest()
            // second call to make sure that worker is enqueued only once
            underTest.enqueueUploadsWorkerRequest()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that uploads worker status info is monitored with monitorUploadsStatusInfo`() =
        runTest {
            whenever(workerClassGateway.uploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueUploadsWorkerRequest()

            underTest.monitorUploadsStatusInfo().test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }
}
