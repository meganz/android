package test.mega.privacy.android.app.cameraupload

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.data.worker.SyncHeartbeatCameraUploadWorker
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import kotlin.Exception
import kotlin.jvm.Throws

/**
 * Test class of [SyncHeartbeatCameraUploadWorker]
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SyncHeartbeatCameraUploadWorkerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private lateinit var context: Context
    private lateinit var underTest: WorkManager

    @Before
    fun setUp() {
        hiltRule.inject()

        context = ApplicationProvider.getApplicationContext()

        val configuration = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        underTest = WorkManager.getInstance(context)
    }

    @Test
    @Throws(Exception::class)
    fun `test that one time sync heartbeat camera upload worker is successful`() {
        // Create the OneTimeWorkRequest
        val request = OneTimeWorkRequestBuilder<SyncHeartbeatCameraUploadWorker>().build()

        // Enqueue and wait for result. This also runs the Worker synchronously
        // due to SynchronousExecutor
        underTest.enqueue(request).result.get()

        // Get WorkInfo
        val workInfo = underTest.getWorkInfoById(request.id).get()

        // Perform Assertion
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.SUCCEEDED)
    }

    @Test
    @Throws(Exception::class)
    fun `test that periodic sync heartbeat camera upload worker is enqueued`() {
        // Create the PeriodicWorkRequest
        val request = PeriodicWorkRequestBuilder<SyncHeartbeatCameraUploadWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = MINUTES,
            flexTimeInterval = 20,
            flexTimeIntervalUnit = MINUTES,
        ).build()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)

        // Enqueue and wait for result. This also runs the Worker synchronously
        // due to SynchronousExecutor
        underTest.enqueue(request).result.get()

        // Tells the testing framework the period delay is met
        testDriver?.setPeriodDelayMet(request.id)

        // Get WorkInfo
        val workInfo = underTest.getWorkInfoById(request.id).get()

        // Perform Assertion
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
    }
}