package mega.privacy.android.feature.sync.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.data.SyncWorker.Companion.SYNC_WORKER_RECHECK_DELAY
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private lateinit var underTest: SyncWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workParams: WorkerParameters
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase
    private val loginMutex: Mutex = mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()

    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        workParams = WorkerParameters(
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
                workDatabase, { _, _ -> }, workExecutor
            )
        )
        whenever(loginMutex.isLocked).thenReturn(true)
        underTest = SyncWorker(
            context,
            workParams,
            monitorSyncsUseCase,
            loginMutex,
            backgroundFastLoginUseCase
        )
    }

    @Test
    fun `test that sync worker finishes immediately if all folders have been synced`() = runTest {
        val firstSync = FolderPair(
            1, "first", "first", RemoteFolder(1232L, "first"), SyncStatus.SYNCED
        )
        val secondSync = FolderPair(
            2, "second", "second", RemoteFolder(2222L, "second"), SyncStatus.SYNCED
        )
        val thirdSync = FolderPair(
            3, "third", "third", RemoteFolder(3333L, "third"), SyncStatus.PAUSED
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(firstSync, secondSync, thirdSync)))

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
    }

    @Test
    fun `test that sync worker is running until all of the folders have been synced`() = runTest {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val firstSync = FolderPair(
            1, "first", "first", RemoteFolder(1232L, "first"), SyncStatus.SYNCING
        )
        val secondSync = FolderPair(
            2, "second", "second", RemoteFolder(1232L, "second"), SyncStatus.SYNCED
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(firstSync, secondSync)))

        val deferredResult = testScope.async { underTest.doWork() }

        testScope.advanceTimeBy(SYNC_WORKER_RECHECK_DELAY)
        whenever(monitorSyncsUseCase()).thenReturn(
            flowOf(
                listOf(
                    firstSync.copy(syncStatus = SyncStatus.SYNCED), secondSync
                )
            )
        )
        testScope.advanceTimeBy(SYNC_WORKER_RECHECK_DELAY)

        val result = deferredResult.await()

        assertEquals(Result.success(), result)
    }
}