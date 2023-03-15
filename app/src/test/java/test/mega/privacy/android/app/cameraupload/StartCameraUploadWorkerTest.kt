package test.mega.privacy.android.app.cameraupload

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.cameraupload.CameraUploadsService
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.worker.StartCameraUploadWorker
import mega.privacy.android.data.wrapper.CameraUploadServiceWrapper
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StartCameraUploadWorkerTest {

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var worker: StartCameraUploadWorker
    private lateinit var workDatabase: WorkDatabase
    private val permissionGateway = mock<PermissionGateway>()
    private val isNotEnoughQuota = mock<IsNotEnoughQuota>()
    private val cameraUploadServiceWrapper = mock<CameraUploadServiceWrapper>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.serialTaskExecutor, true)
        cameraUploadServiceWrapper.stub {
            on { newIntent(context) }.thenReturn(Intent(context, CameraUploadsService::class.java))
        }

        worker = StartCameraUploadWorker(
            context,
            WorkerParameters(
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
                WorkForegroundUpdater(workDatabase, object : ForegroundProcessor {
                    override fun startForeground(
                        workSpecId: String,
                        foregroundInfo: ForegroundInfo,
                    ) {
                    }

                    override fun stopForeground(workSpecId: String) {}
                    override fun isEnqueuedInForeground(workSpecId: String): Boolean = true
                }, workExecutor)
            ),
            cameraUploadServiceWrapper,
            permissionGateway,
            isNotEnoughQuota,
        )
    }

    @Test
    fun `test that camera upload worker is started successfully if the read external permission is granted, the user is not over quota`() =
        runTest {
            whenever(permissionGateway.hasPermissions(anyVararg())).thenReturn(true)
            whenever(isNotEnoughQuota()).thenReturn(false)
            val result = worker.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the camera upload worker fails to start if read external storage permission is not granted`() =
        runTest {
            whenever(permissionGateway.hasPermissions(anyVararg())).thenReturn(false)
            whenever(isNotEnoughQuota()).thenReturn(false)
            val result = worker.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that the camera upload worker fails to start if the user is over quota`() = runTest {
        whenever(permissionGateway.hasPermissions(anyVararg())).thenReturn(true)
        whenever(isNotEnoughQuota()).thenReturn(true)
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
