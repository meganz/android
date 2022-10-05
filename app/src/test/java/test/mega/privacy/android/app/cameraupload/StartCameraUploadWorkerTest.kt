package test.mega.privacy.android.app.cameraupload

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.content.Context
import android.os.Build
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
import mega.privacy.android.app.utils.JobUtil.SHOULD_IGNORE_ATTRIBUTES
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

    private lateinit var permissions: Array<String>

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
                workDataOf(SHOULD_IGNORE_ATTRIBUTES to true),
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
            TestWrapperModule.cameraUploadsServiceWrapper
        )

        whenever(TestWrapperModule.permissionUtilWrapper.getImagePermissionByVersion()).thenReturn(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                READ_MEDIA_IMAGES
            } else {
                READ_EXTERNAL_STORAGE
            }
        )

        whenever(TestWrapperModule.permissionUtilWrapper.getVideoPermissionByVersion()).thenReturn(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                READ_MEDIA_VIDEO
            } else {
                READ_EXTERNAL_STORAGE
            }
        )

        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                TestWrapperModule.permissionUtilWrapper.getImagePermissionByVersion(),
                TestWrapperModule.permissionUtilWrapper.getVideoPermissionByVersion()
            )
        } else {
            arrayOf(READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
        }
    }

    @Test
    fun `test that camera upload worker is started successfully if the read external permission is granted, the user is not over quota and the camera upload service is not yet started`() {

        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(
                context,
                *permissions
            )
        ).thenReturn(true)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(false)
        whenever(TestWrapperModule.cameraUploadsServiceWrapper.isServiceRunning()).thenReturn(
            false
        )
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that the camera upload worker fails to start if read external storage permission is not granted`() {
        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(
                context,
                *permissions
            )
        ).thenReturn(false)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(false)
        whenever(TestWrapperModule.cameraUploadsServiceWrapper.isServiceRunning()).thenReturn(
            false
        )
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that the camera upload worker fails to start if the user is over quota`() {
        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(
                context,
                *permissions
            )
        ).thenReturn(true)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(true)
        whenever(TestWrapperModule.cameraUploadsServiceWrapper.isServiceRunning()).thenReturn(
            false
        )
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that the camera upload worker fails to start if the camera upload service is already running`() {
        whenever(
            TestWrapperModule.permissionUtilWrapper.hasPermissions(
                context,
                *permissions
            )
        ).thenReturn(true)
        whenever(TestWrapperModule.jobUtilWrapper.isOverQuota()).thenReturn(false)
        whenever(TestWrapperModule.cameraUploadsServiceWrapper.isServiceRunning()).thenReturn(
            true
        )
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
