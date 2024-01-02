package test.mega.privacy.android.app.cameraupload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.worker.SyncHeartbeatCameraUploadWorker
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.camerauploads.SendCameraUploadsBackupHeartBeatUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendMediaUploadsBackupHeartBeatUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Test class of [SyncHeartbeatCameraUploadWorker]
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SyncHeartbeatCameraUploadWorkerTest {
    private lateinit var underTest: SyncHeartbeatCameraUploadWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val applicationWrapper = mock<ApplicationWrapper>()
    private val backgroundFastLoginUseCase = mock<BackgroundFastLoginUseCase>()
    private val sendCameraUploadsBackupHeartBeatUseCase =
        mock<SendCameraUploadsBackupHeartBeatUseCase>()
    private val sendMediaUploadsBackupHeartBeatUseCase =
        mock<SendMediaUploadsBackupHeartBeatUseCase>()
    private val loginMutex = mock<Mutex>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        underTest = SyncHeartbeatCameraUploadWorker(
            context = context,
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            applicationWrapper = applicationWrapper,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            sendCameraUploadsBackupHeartBeatUseCase = sendCameraUploadsBackupHeartBeatUseCase,
            sendMediaUploadsBackupHeartBeatUseCase = sendMediaUploadsBackupHeartBeatUseCase,
            loginMutex = loginMutex,
        )
    }

    @Test
    fun `test that both primary and secondary backup heartbeats are sent`() = runTest {
        whenever(loginMutex.isLocked).thenReturn(false)
        val result = underTest.doWork()

        val inOrder = inOrder(
            backgroundFastLoginUseCase,
            applicationWrapper,
            sendCameraUploadsBackupHeartBeatUseCase,
            sendMediaUploadsBackupHeartBeatUseCase,
        )

        inOrder.verify(backgroundFastLoginUseCase).invoke()
        inOrder.verify(applicationWrapper).setHeartBeatAlive(true)
        inOrder.verify(sendCameraUploadsBackupHeartBeatUseCase).invoke(
            heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
            lastNodeHandle = -1L
        )
        inOrder.verify(sendMediaUploadsBackupHeartBeatUseCase).invoke(
            heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
            lastNodeHandle = -1L
        )

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that a failure is returned when an exception is found`() = runTest {
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
