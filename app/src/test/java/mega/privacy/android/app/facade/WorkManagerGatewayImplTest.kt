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
            workManager = workManager,
            crashReporter = crashReporter,
            workerClassGateway = workerClassGateway,
        )
    }

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
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
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
    fun `test that new media worker is not enqueued again when calling enqueueNewMediaWorkerRequest with forceEnqueue false and another new media worker is already queued or running`() =
        runTest {
            val tag = "NEW_MEDIA_WORKER_TAG"
            whenever(workerClassGateway.newMediaWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueNewMediaWorkerRequest(false)
            // second call to make sure that worker is enqueued only once
            underTest.enqueueNewMediaWorkerRequest(false)

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
            }
        }

    @Test
    fun `test that new media worker is enqueued when calling enqueueNewMediaWorkerRequest with forceEnqueue true`() =
        runTest {
            val tag = "NEW_MEDIA_WORKER_TAG"
            whenever(workerClassGateway.newMediaWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueNewMediaWorkerRequest(false)
            underTest.enqueueNewMediaWorkerRequest(true)

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(2)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                assertThat(info[1].state).isEqualTo(WorkInfo.State.ENQUEUED)
            }
        }

    @Test
    fun `test that camera uploads worker is enqueued only once when calling startCameraUploads`() =
        runTest {
            val tag = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.startCameraUploads()
            // second call to make sure that worker is enqueued only once
            underTest.startCameraUploads()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that periodic camera uploads worker is enqueued only once with correct tag when calling scheduleCameraUploads`() =
        runTest {
            val tag = "CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()
            // second call to make sure that worker is enqueued only once
            underTest.scheduleCameraUploads()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
            }
        }

    @Test
    fun `test that periodic camera uploads worker will run after interval`() =
        runTest {
            val tag = "CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                WorkManagerTestInitHelper.getTestDriver(context)?.apply {
                    setInitialDelayMet(info[0].id)
                    setPeriodDelayMet(info[0].id)
                }
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that periodic heartbeat worker is enqueued only once when calling scheduleCameraUploads`() =
        runTest {
            val tag = "HEART_BEAT_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()
            // second call to make sure that worker is enqueued only once
            underTest.scheduleCameraUploads()

            val workInfo = workManager.getWorkInfosByTagFlow(tag)
            workInfo.test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that single camera uploads worker is cancelled when calling stopCameraUploads`() =
        runTest {
            val singleTag = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.startCameraUploads()

            val workInfoSingleWorker = workManager.getWorkInfosByTagFlow(singleTag)
            workInfoSingleWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.RUNNING)
                underTest.stopCameraUploads()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }

    @Test
    fun `test that periodic camera uploads worker is cancelled when calling stopCameraUploads`() =
        runTest {
            val tag = "CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()

            val workInfoPeriodicWorker = workManager.getWorkInfosByTagFlow(tag)
            workInfoPeriodicWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                underTest.stopCameraUploads()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }

    @Test
    fun `test that single camera uploads worker is cancelled when calling cancelCameraUploadsAndHeartbeatWorkRequest`() =
        runTest {
            val singleTag = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.startCameraUploads()

            val workInfoSingleWorker = workManager.getWorkInfosByTagFlow(singleTag)
            workInfoSingleWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.RUNNING)
                underTest.cancelCameraUploadsAndHeartbeatWorkRequest()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }

    @Test
    fun `test that periodic camera uploads worker is cancelled when calling cancelCameraUploadsAndHeartbeatWorkRequest`() =
        runTest {
            val tag = "CAMERA_UPLOAD_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()

            val workInfoPeriodicWorker = workManager.getWorkInfosByTagFlow(tag)
            workInfoPeriodicWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                underTest.cancelCameraUploadsAndHeartbeatWorkRequest()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }

    @Test
    fun `test that new media worker is cancelled when calling cancelCameraUploadsAndHeartbeatWorkRequest`() =
        runTest {
            val newMediaTag = "NEW_MEDIA_WORKER_TAG"
            whenever(workerClassGateway.newMediaWorkerClass)
                .thenReturn(fakeWorker)

            underTest.enqueueNewMediaWorkerRequest(false)

            val workInfoNewMediaWorker = workManager.getWorkInfosByTagFlow(newMediaTag)
            workInfoNewMediaWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                underTest.cancelCameraUploadsAndHeartbeatWorkRequest()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }

    @Test
    fun `test that heartbeat worker is cancelled when calling cancelCameraUploadsAndHeartbeatWorkRequest`() =
        runTest {
            val heartBeatTag = "HEART_BEAT_TAG"
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()

            val workInfoHeartbeatWorker = workManager.getWorkInfosByTagFlow(heartBeatTag)
            workInfoHeartbeatWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.RUNNING)
                underTest.cancelCameraUploadsAndHeartbeatWorkRequest()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
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
    fun `test that single camera uploads worker status info is monitored with monitorCameraUploadsStatusInfo`() =
        runTest {
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)

            underTest.startCameraUploads()

            underTest.monitorCameraUploadsStatusInfo().test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.RUNNING)
            }
        }

    @Test
    fun `test that periodic camera uploads worker status info is monitored with monitorCameraUploadsStatusInfo`() =
        runTest {
            whenever(workerClassGateway.cameraUploadsWorkerClass)
                .thenReturn(fakeWorker)
            whenever(workerClassGateway.syncHeartbeatCameraUploadWorkerClass)
                .thenReturn(fakeWorker)

            underTest.scheduleCameraUploads()

            underTest.monitorCameraUploadsStatusInfo().test {
                val info = awaitItem()
                assertThat(info.size).isEqualTo(1)
                assertThat(info[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
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
