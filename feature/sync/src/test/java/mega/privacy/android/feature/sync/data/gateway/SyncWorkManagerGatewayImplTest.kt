package mega.privacy.android.feature.sync.data.gateway

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.feature.sync.data.SyncWorker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class SyncWorkManagerGatewayImplTest {

    private lateinit var underTest: SyncWorkManagerGatewayImpl

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private var crashReporter = mock<CrashReporter>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        workManager = WorkManager.getInstance(context)

        underTest = SyncWorkManagerGatewayImpl(
            workManager = workManager,
            crashReporter = crashReporter,
        )
    }

    @Test
    fun `test that gateway starts and cancels sync worker`() =
        runTest {
            underTest.enqueueSyncWorkerRequest(
                frequencyInMinutes = 15,
                networkType = NetworkType.UNMETERED
            )

            val syncWorker = workManager.getWorkInfosByTagFlow(SyncWorker.SYNC_WORKER_TAG)
            syncWorker.test {
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
                underTest.cancelSyncWorkerRequest()
                assertThat(awaitItem()[0].state).isEqualTo(WorkInfo.State.CANCELLED)
            }
        }
}