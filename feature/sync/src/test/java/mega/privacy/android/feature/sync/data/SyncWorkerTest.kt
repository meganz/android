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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.worker.ForegroundSetter
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.data.SyncWorker.Companion.SYNC_WORKER_RECHECK_DELAY_IN_SECONDS
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SetSyncWorkerForegroundPreferenceUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    private val loginMutex: Mutex = mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase = mock()
    private val monitorSyncNotificationsUseCase: MonitorSyncNotificationsUseCase = mock()
    private val syncNotificationManager: SyncNotificationManager = mock()
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase = mock()
    private val isRootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase =
        mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase =
        mock()
    private val getSyncWorkerForegroundPreferenceUseCase: GetSyncWorkerForegroundPreferenceUseCase =
        mock()
    private val foregroundSetter: ForegroundSetter = mock()

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
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))
        whenever(monitorSyncNotificationsUseCase()).thenReturn(emptyFlow())
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(emptyList()))
        underTest = SyncWorker(
            context = context,
            workerParams = workParams,
            monitorSyncsUseCase = monitorSyncsUseCase,
            loginMutex = loginMutex,
            monitorShouldSyncUseCase = monitorShouldSyncUseCase,
            monitorSyncNotificationsUseCase = monitorSyncNotificationsUseCase,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            syncNotificationManager = syncNotificationManager,
            setSyncNotificationShownUseCase = setSyncNotificationShownUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase = pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            isRootNodeExistsUseCase = isRootNodeExistsUseCase,
            syncPermissionManager = syncPermissionsManager,
            setSyncWorkerForegroundPreferenceUseCase = setSyncWorkerForegroundPreferenceUseCase,
            getSyncWorkerForegroundPreferenceUseCase = getSyncWorkerForegroundPreferenceUseCase,
            foregroundSetter = foregroundSetter
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
    fun `test that sync worker dispatches notifications`() = runTest {
        val notification: SyncNotificationMessage = mock()
        whenever(monitorSyncNotificationsUseCase()).thenReturn(flowOf(notification))
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
        whenever(syncNotificationManager.isSyncNotificationDisplayed()).thenReturn(false)
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        underTest.doWork()

        verify(syncNotificationManager).show(context, notification)
    }

    @Test
    fun `test that sync worker retries if login fails`() = runTest {
        whenever(loginMutex.isLocked).thenReturn(false) // Simulate login lock
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException("Login failed"))

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.retry())
    }

    @Test
    fun `test that fast login mutex is waited at least 3 times and then sync worker retries`() =
        runTest {
            whenever(loginMutex.isLocked).thenReturn(true) // Simulate login lock
            whenever(isRootNodeExistsUseCase()).thenReturn(false)
            val result = underTest.doWork()

            assertThat(result).isEqualTo(Result.retry())
            verifyNoInteractions(backgroundFastLoginUseCase)
        }

    @Test
    fun `test that no notification is displayed if permission is denied`() = runTest {
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(false)
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        underTest.doWork()

        verify(syncNotificationManager, never()).show(any(), any())
    }

    @Test
    fun `test that syncs are paused when should be paused`() = runTest {
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(false))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)
        underTest.doWork()

        verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(false)
    }

    @Test
    fun `test that syncs are resumed when should be resumed`() = runTest {
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)
        underTest.doWork()
        verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(true)
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
    fun `test isLoginSuccessful returns false when exception is thrown and worker retries`() =
        runTest {
            whenever(loginMutex.isLocked).thenReturn(false)
            whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException("Login failed"))

            val result = underTest.doWork()

            assertThat(result).isEqualTo(Result.retry())
        }

    @Test
    fun `test that sync worker skips notification display when already displayed`() = runTest {
        val notification: SyncNotificationMessage = mock()
        whenever(monitorSyncNotificationsUseCase()).thenReturn(flowOf(notification))
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
        whenever(syncNotificationManager.isSyncNotificationDisplayed()).thenReturn(true)
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        underTest.doWork()

        verify(syncNotificationManager, never()).show(any(), any())
        verify(setSyncNotificationShownUseCase).invoke(notification, null)
    }

    @Test
    fun `test that sync worker waits for empty syncs list before checking completion`() = runTest {
        // First emit empty list, then after delay emit completed syncs
        whenever(monitorSyncsUseCase()).thenReturn(
            flow {
                emit(emptyList())
                delay(SYNC_WORKER_RECHECK_DELAY_IN_SECONDS.seconds + 100.seconds)
                emit(
                    listOf(
                        FolderPair(
                            id = 1,
                            syncType = SyncType.TYPE_TWOWAY,
                            pairName = "test",
                            localFolderPath = "test",
                            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
                            syncStatus = SyncStatus.SYNCED
                        )
                    )
                )
            }
        )
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(Result.success())
    }

    @Test
    fun `test that sync worker runs in foreground when preference is true and promotion succeeds`() =
        runTest {
            val syncedSync = FolderPair(
                id = 1,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "test",
                localFolderPath = "test",
                remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
                syncStatus = SyncStatus.SYNCED
            )
            whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(syncedSync)))
            whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
            whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
            whenever(syncNotificationManager.createForegroundNotification(context)).thenReturn(
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
        val syncedSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "test",
            localFolderPath = "test",
            remoteFolder = RemoteFolder(id = NodeId(1232L), name = "test"),
            syncStatus = SyncStatus.SYNCED
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(syncedSync)))
        whenever(getSyncWorkerForegroundPreferenceUseCase()).thenReturn(true)
        whenever(syncNotificationManager.createForegroundNotification(context)).thenReturn(
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
}
