package test.mega.privacy.android.app.cameraupload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.jobservices.StartCameraUploadWorker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.di.TestWrapperModule
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class StartCameraUploadWorkerTest {

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var worker: StartCameraUploadWorker
    private lateinit var workDatabase: WorkDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.backgroundExecutor, true)

        worker = StartCameraUploadWorker(
            context,
            WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase, object : ForegroundProcessor {
                    override fun startForeground(
                        workSpecId: String,
                        foregroundInfo: ForegroundInfo,
                    ) {
                    }

                    override fun stopForeground(workSpecId: String) {}
                }, workExecutor)
            ),
            TestWrapperModule.permissionUtilWrapper,
            TestWrapperModule.jobUtilWrapper,
        )
    }

    @Test
    fun `test that camera upload worker is started successfully if the read external permission is granted, the user is not over quota`() {

        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(anyVararg())
        ).thenReturn(true)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(false)
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that the camera upload worker fails to start if read external storage permission is not granted`() {
        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(anyVararg())
        ).thenReturn(false)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(false)
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that the camera upload worker fails to start if the user is over quota`() {
        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(anyVararg())
        ).thenReturn(true)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(true)
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
