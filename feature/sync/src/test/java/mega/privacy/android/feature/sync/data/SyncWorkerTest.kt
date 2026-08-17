package mega.privacy.android.feature.sync.data

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.DefaultWorkerFactory
import androidx.work.ListenableWorker.Result
import androidx.work.SystemClock
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.worker.ForegroundSetter
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.data.SyncWorker.Companion.SYNC_WORKER_RECHECK_DELAY_IN_SECONDS
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class SyncWorkerTest {

    private lateinit var underTest: SyncWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workParams: WorkerParameters
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val syncNotificationManager: SyncNotificationManager = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase =
        mock()
    private val getSyncWorkerForegroundPreferenceUseCase: GetSyncWorkerForegroundPreferenceUseCase =
        mock()
    private val foregroundSetter: ForegroundSetter = mock()
    private val crashReporter: CrashReporter = mock()

    @Before
    fun setUp() {
        context = mock()
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
            Dispatchers.Unconfined,
            workExecutor,
            DefaultWorkerFactory,
            WorkProgressUpdater(workDatabase, workExecutor),
            WorkForegroundUpdater(
                workDatabase, { _, _ -> }, workExecutor
            )
        )
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(emptyList()))
        underTest = SyncWorker(
            context = context,
            workerParams = workParams,
            monitorSyncsUseCase = monitorSyncsUseCase,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            syncNotificationManager = syncNotificationManager,
            syncPermissionManager = syncPermissionsManager,
            setSyncWorkerForegroundPreferenceUseCase = setSyncWorkerForegroundPreferenceUseCase,
            getSyncWorkerForegroundPreferenceUseCase = getSyncWorkerForegroundPreferenceUseCase,
            foregroundSetter = foregroundSetter,
            crashReporter = crashReporter
        )
    }

    @Test
    fun `test that sync worker finishes immediately if all folders have been synced`() = runTest {
        val firstSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "first",
            localFolderPath = "first",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "first"),
            syncStatus = SyncStatus.SYNCED
        )
        val secondSync = FolderPair(
            id = 2,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "second",
            localFolderPath = "second",
            remoteFolder = RemoteFolder(id = NodeId(2222L), name = "second"),
            syncStatus = SyncStatus.SYNCED
        )
        val thirdSync = FolderPair(
            id = 3,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "third",
            localFolderPath = "third",
            remoteFolder = RemoteFolder(id = NodeId(3333L), name = "third"),
            syncStatus = SyncStatus.PAUSED
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(firstSync, secondSync, thirdSync)))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker finishes when all folders are terminal`() = runTest {
        val errorSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "error",
            localFolderPath = "error",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "error"),
            syncStatus = SyncStatus.ERROR,
        )
        val disabledSync = errorSync.copy(
            id = 2,
            pairName = "disabled",
            localFolderPath = "disabled",
            syncStatus = SyncStatus.DISABLED,
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(errorSync, disabledSync)))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker is running until all of the folders have been synced`() = runTest {
        val firstSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "first",
            localFolderPath = "first",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "first"),
            syncStatus = SyncStatus.SYNCING
        )
        val secondSync = FolderPair(
            id = 2,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "second",
            localFolderPath = "second",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "second"),
            syncStatus = SyncStatus.SYNCED
        )

        // Create a flow that emits the initial state, then after a delay emits the completed state
        whenever(monitorSyncsUseCase()).thenReturn(
            flow {
                emit(listOf(firstSync, secondSync))
                delay(SYNC_WORKER_RECHECK_DELAY_IN_SECONDS.seconds + 100.seconds) // Wait a bit longer than the recheck delay
                emit(listOf(firstSync.copy(syncStatus = SyncStatus.SYNCED), secondSync))
            }
        )
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker retries if login fails`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException("Login failed"))

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.retry())
    }

    @Test
    fun `test that sync worker delegates the login attempt to the fast login use case`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(emptyFlow())
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        underTest.doWork()

        // The use case owns the login mutex and the already-logged-in short circuit, so the
        // worker must not gate the call behind its own checks.
        verify(backgroundFastLoginUseCase).invoke()
    }

    @Test
    fun `test that sync worker sets preference to true on timeout`() = runTest {
        // Create a flow that never completes (always syncing)
        whenever(monitorSyncsUseCase()).thenReturn(
            flow {
                while (true) {
                    emit(listOf(mock<FolderPair>()))
                    delay(1000)
                }
            }
        )
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.retry())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(true)
    }

    @Test
    fun `test that sync worker completes when no syncs remain`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(emptyFlow())
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker completes when syncs disappear`() = runTest {
        val syncingSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "test",
            localFolderPath = "test",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
            syncStatus = SyncStatus.SYNCING,
        )
        whenever(monitorSyncsUseCase()).thenReturn(
            flow {
                emit(listOf(syncingSync))
                emit(emptyList())
            }
        )
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker completes when all remaining work is stalled`() = runTest {
        val syncingSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "test",
            localFolderPath = "test",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
            syncStatus = SyncStatus.SYNCING,
        )
        val stalledIssue = StalledIssue(
            id = "issue-id",
            syncId = syncingSync.id,
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("test/file"),
            issueType = StallIssueType.FileIssue,
            conflictName = "file",
            nodeNames = listOf("file"),
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(syncingSync)))
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(listOf(stalledIssue)))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker does not promote stalled work to foreground`() = runTest {
        val syncingSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "test",
            localFolderPath = "test",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
            syncStatus = SyncStatus.SYNCING,
        )
        val stalledIssue = StalledIssue(
            id = "issue-id",
            syncId = syncingSync.id,
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("test/file"),
            issueType = StallIssueType.FileIssue,
            conflictName = "file",
            nodeNames = listOf("file"),
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(syncingSync)))
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(listOf(stalledIssue)))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
        verifyNoInteractions(foregroundSetter)
    }

    @Test
    fun `test that sync worker does not wait indefinitely for foreground state before login`() =
        runTest {
            whenever(monitorSyncsUseCase()).thenReturn(flow { awaitCancellation() })
            whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow { awaitCancellation() })
            whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
            whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(Result.success())
            verifyNoInteractions(foregroundSetter)
        }

    @Test
    fun `test that sync worker runs in foreground when preference is true and promotion succeeds`() =
        runTest {
            val syncingSync = FolderPair(
                id = 1,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "test",
                localFolderPath = "test",
                remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
                syncStatus = SyncStatus.SYNCING
            )
            whenever(monitorSyncsUseCase()).thenReturn(
                flowOf(
                    listOf(syncingSync),
                    listOf(syncingSync.copy(syncStatus = SyncStatus.SYNCED)),
                )
            )
            whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
            whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
            whenever(syncNotificationManager.createForegroundNotification()).thenReturn(
                mock()
            )

            val result = underTest.doWork()

            assertThat(result).isEqualTo(Result.success())
            verify(foregroundSetter).setForeground(any())
            verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
        }

    @Test
    fun `test that sync worker skips running in foreground when notification permission is not granted`() =
        runTest {
            whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(false)

            underTest.doWork()

            verifyNoInteractions(foregroundSetter)
        }

    @Test
    fun `test that sync worker handles foreground promotion failure gracefully`() = runTest {
        val syncingSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "test",
            localFolderPath = "test",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
            syncStatus = SyncStatus.SYNCING
        )
        whenever(monitorSyncsUseCase()).thenReturn(
            flowOf(
                listOf(syncingSync),
                listOf(syncingSync.copy(syncStatus = SyncStatus.SYNCED)),
            )
        )
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
        whenever(syncNotificationManager.createForegroundNotification()).thenReturn(
            mock()
        )
        whenever(foregroundSetter.setForeground(any())).thenThrow(
            ForegroundServiceStartNotAllowedException("Foreground not allowed")
        )

        val result = underTest.doWork()

        // Should still complete successfully even if foreground promotion fails
        assertThat(result).isEqualTo(Result.success())
        verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
    }

    @Test
    fun `test that sync worker keeps running in foreground after initial timeout until sync completes`() =
        runTest {
            val syncingSync = FolderPair(
                id = 1,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "test",
                localFolderPath = "test",
                remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
                syncStatus = SyncStatus.SYNCING
            )

            // Create a flow that emits syncing status and never completes
            whenever(monitorSyncsUseCase()).thenReturn(
                flow {
                    emit(listOf(syncingSync))
                    // Keep emitting to simulate ongoing sync
                    while (true) {
                        delay(1000)
                        emit(listOf(syncingSync))
                    }
                }
            )
            whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false).thenReturn(true)
                .thenReturn(true)

            val deferredBackground = async {
                underTest.doWork()
            }

            // Let it run for a 10 minutes then wait for the result
            advanceTimeBy(10.minutes)
            val backgroundWorkerResult = deferredBackground.await()

            // The worker should handle cancellation and any subsequent exception should result in retry
            assertThat(backgroundWorkerResult).isEqualTo(Result.retry())
            verify(setSyncWorkerForegroundPreferenceUseCase).invoke(true)

            val deferredForeground = async {
                underTest.doWork()
            }

            // Let it run for a 1 hour then wait for the result
            advanceTimeBy(1.hours)
            val foregroundWorkerResult = deferredForeground.await()
            assertThat(foregroundWorkerResult).isEqualTo(Result.retry())
            verify(setSyncWorkerForegroundPreferenceUseCase, times(2)).invoke(true)

            reset(monitorSyncsUseCase)
            // Create a flow that emits syncing status and  completes
            whenever(monitorSyncsUseCase()).thenReturn(
                flow {
                    emit(listOf(syncingSync.copy(syncStatus = SyncStatus.SYNCED, id = 2)))
                    awaitCancellation()
                }
            )

            val deferredForegroundCompleted = async {
                underTest.doWork()
            }

            advanceTimeBy(30.minutes)

            val foregroundCompletedWorkerResult = deferredForegroundCompleted.await()
            assertThat(foregroundCompletedWorkerResult).isEqualTo(Result.success())
            verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
        }

    @Test
    fun `test that a cancelled sync worker rethrows cancellation instead of retrying`() = runTest {
        // Simulate the worker being cancelled while it is doing its work.
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
        whenever(getSyncWorkerForegroundPreferenceUseCase())
            .thenThrow(CancellationException("Worker stopped"))

        val outcome = runCatching { underTest.doWork() }

        // Cancellation must propagate so WorkManager treats it as a stop, not silently retry.
        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `test that cancellation during login propagates instead of retrying`() = runTest {
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)
        whenever(backgroundFastLoginUseCase())
            .thenThrow(CancellationException("Worker stopped during login"))

        val outcome = runCatching { underTest.doWork() }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
