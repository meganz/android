package mega.privacy.android.data.worker


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.offline.SyncOfflineFilesUseCase
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Test class of [OfflineSyncWorker]
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineSyncWorkerTest {
    private lateinit var underTest: OfflineSyncWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val syncOfflineFilesUseCase = mock<SyncOfflineFilesUseCase>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(
            context,
            workExecutor.serialTaskExecutor,
            clock = SystemClock(),
            useTestDatabase = true
        )
        underTest = OfflineSyncWorker(
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
                WorkForegroundUpdater(
                    workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            syncOfflineFilesUseCase = syncOfflineFilesUseCase,
        )
    }

    @Test
    fun `test that offline sync is started when the worker is running`() = runTest {
        underTest.doWork()

        verify(syncOfflineFilesUseCase).invoke()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            syncOfflineFilesUseCase
        )
    }
}
