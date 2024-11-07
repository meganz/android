package mega.privacy.android.feature.sync.data

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.data.SyncWorker.Companion.SYNC_WORKER_RECHECK_DELAY
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()
    private val syncNotificationManager: SyncNotificationManager = mock()
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()

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
        whenever(loginMutex.isLocked).thenReturn(true)
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(mock()))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(false))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        underTest = SyncWorker(
            context,
            workParams,
            monitorSyncsUseCase,
            loginMutex,
            backgroundFastLoginUseCase,
            monitorSyncStalledIssuesUseCase,
            monitorBatteryInfoUseCase,
            monitorSyncByWiFiUseCase,
            monitorConnectivityUseCase,
            getSyncNotificationUseCase,
            isOnWifiNetworkUseCase,
            syncNotificationManager,
            setSyncNotificationShownUseCase,
            syncPermissionsManager
        )
    }

    @Test
    fun `test that sync worker finishes immediately if all folders have been synced`() = runTest {
        val firstSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "first",
            localFolderPath = "first",
            remoteFolder = RemoteFolder(id = 1232L, name = "first"),
            syncStatus = SyncStatus.SYNCED
        )
        val secondSync = FolderPair(
            id = 2,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "second",
            localFolderPath = "second",
            remoteFolder = RemoteFolder(id = 2222L, name = "second"),
            syncStatus = SyncStatus.SYNCED
        )
        val thirdSync = FolderPair(
            id = 3,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "third",
            localFolderPath = "third",
            remoteFolder = RemoteFolder(id = 3333L, name = "third"),
            syncStatus = SyncStatus.PAUSED
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
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "first",
            localFolderPath = "first",
            remoteFolder = RemoteFolder(id = 1232L, name = "first"),
            syncStatus = SyncStatus.SYNCING
        )
        val secondSync = FolderPair(
            id = 2,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "second",
            localFolderPath = "second",
            remoteFolder = RemoteFolder(id = 1232L, name = "second"),
            syncStatus = SyncStatus.SYNCED
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

    @Test
    fun `test that sync worker dispatches notifications`() = runTest {
        val notification: SyncNotificationMessage = mock()
        val firstSync = FolderPair(
            id = 1,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "first",
            localFolderPath = "first",
            remoteFolder = RemoteFolder(id = 1232L, name = "first"),
            syncStatus = SyncStatus.SYNCED,
            syncError = SyncError.ACTIVE_SYNC_SAME_PATH
        )
        val secondSync = FolderPair(
            id = 2,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "second",
            localFolderPath = "second",
            remoteFolder = RemoteFolder(id = 2222L, name = "second"),
            syncStatus = SyncStatus.SYNCED
        )
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(firstSync, secondSync)))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(100, false)))
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = listOf(firstSync, secondSync),
                stalledIssues = emptyList()
            )
        ).thenReturn(notification)
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)

        underTest.doWork()

        verify(syncNotificationManager).show(context, notification)
    }
}