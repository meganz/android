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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.cameraupload.CameraUploadsWorker
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.backup.InitializeBackupsUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastCameraUploadsSettingsActionUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.camerauploads.DoesCameraUploadsRecordExistsInTargetNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.ExtractGpsCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPendingCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.RenameCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.UploadCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.CancelAllUploadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * Test class of [CameraUploadsWorker]
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CameraUploadsWorkerTest {

    private lateinit var underTest: CameraUploadsWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val isNotEnoughQuota: IsNotEnoughQuota = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase = mock()
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase = mock()
    private val isSecondaryFolderSetUseCase: IsSecondaryFolderSetUseCase = mock()
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase = mock()
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase = mock()
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase = mock()
    private val isChargingRequired: IsChargingRequired = mock()
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase = mock()
    private val setPrimarySyncHandle: SetPrimarySyncHandle = mock()
    private val setSecondarySyncHandle: SetSecondarySyncHandle = mock()
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase = mock()
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorBatteryInfo: MonitorBatteryInfo = mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase = mock()
    private val monitorChargingStoppedState: MonitorChargingStoppedState = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val handleLocalIpChangeUseCase: HandleLocalIpChangeUseCase = mock()
    private val cancelAllUploadTransfersUseCase: CancelAllUploadTransfersUseCase = mock()
    private val createCameraUploadFolder: CreateCameraUploadFolder = mock()
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase = mock()
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase = mock()
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase =
        mock()
    private val resetTotalUploadsUseCase: ResetTotalUploadsUseCase = mock()
    private val disableMediaUploadSettings: DisableMediaUploadSettings = mock()
    private val createCameraUploadTemporaryRootDirectoryUseCase: CreateCameraUploadTemporaryRootDirectoryUseCase =
        mock()
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase =
        mock()
    private val broadcastCameraUploadProgress: BroadcastCameraUploadProgress = mock()
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase = mock()

    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase =
        mock()
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase = mock()
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase =
        mock()
    private val isChargingUseCase: IsChargingUseCase = mock()
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase = mock()
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase = mock()
    private val cameraUploadsNotificationManagerWrapper: CameraUploadsNotificationManagerWrapper =
        mock()
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase = mock()
    private val cookieEnabledCheckWrapper: CookieEnabledCheckWrapper = mock()
    private val broadcastCameraUploadsSettingsActionUseCase: BroadcastCameraUploadsSettingsActionUseCase =
        mock()
    private var isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val processCameraUploadsMediaUseCase: ProcessCameraUploadsMediaUseCase = mock()
    private val getPendingCameraUploadsRecordsUseCase: GetPendingCameraUploadsRecordsUseCase =
        mock()
    private val renameCameraUploadsRecordsUseCase: RenameCameraUploadsRecordsUseCase = mock()
    private val doesCameraUploadsRecordExistsInTargetNodeUseCase: DoesCameraUploadsRecordExistsInTargetNodeUseCase =
        mock()
    private val extractGpsCoordinatesUseCase: ExtractGpsCoordinatesUseCase = mock()
    private val uploadCameraUploadsRecordsUseCase: UploadCameraUploadsRecordsUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val initializeBackupsUseCase: InitializeBackupsUseCase = mock()
    private val areCameraUploadsFoldersInRubbishBinUseCase: AreCameraUploadsFoldersInRubbishBinUseCase =
        mock()
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase = mock()
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase = mock()
    private val loginMutex: Mutex = mock()

    private val foregroundInfo = ForegroundInfo(1, mock())
    private val primaryNodeHandle = 1111L
    private val secondaryNodeHandle = -1L
    private val primaryLocalPath = "primaryPath"


    @Before
    fun setUp() {
        Dispatchers.setMain(ioDispatcher)

        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.serialTaskExecutor, true)
        underTest = Mockito.spy(
            CameraUploadsWorker(
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
                isNotEnoughQuota = isNotEnoughQuota,
                getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
                isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
                isSecondaryFolderSetUseCase = isSecondaryFolderSetUseCase,
                isSecondaryFolderEnabled = isSecondaryFolderEnabled,
                isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
                isWifiNotSatisfiedUseCase = isWifiNotSatisfiedUseCase,
                setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
                setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
                isChargingRequired = isChargingRequired,
                getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
                setPrimarySyncHandle = setPrimarySyncHandle,
                setSecondarySyncHandle = setSecondarySyncHandle,
                getDefaultNodeHandleUseCase = getDefaultNodeHandleUseCase,
                ioDispatcher = ioDispatcher,
                monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                monitorBatteryInfo = monitorBatteryInfo,
                backgroundFastLoginUseCase = backgroundFastLoginUseCase,
                isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
                monitorChargingStoppedState = monitorChargingStoppedState,
                monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
                handleLocalIpChangeUseCase = handleLocalIpChangeUseCase,
                cancelAllUploadTransfersUseCase = cancelAllUploadTransfersUseCase,
                createCameraUploadFolder = createCameraUploadFolder,
                setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
                setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
                establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
                resetTotalUploadsUseCase = resetTotalUploadsUseCase,
                disableMediaUploadSettings = disableMediaUploadSettings,
                createCameraUploadTemporaryRootDirectoryUseCase = createCameraUploadTemporaryRootDirectoryUseCase,
                deleteCameraUploadsTemporaryRootDirectoryUseCase = deleteCameraUploadsTemporaryRootDirectoryUseCase,
                broadcastCameraUploadProgress = broadcastCameraUploadProgress,
                scheduleCameraUploadUseCase = scheduleCameraUploadUseCase,
                updateCameraUploadsBackupStatesUseCase = updateCameraUploadsBackupStatesUseCase,
                sendBackupHeartBeatSyncUseCase = sendBackupHeartBeatSyncUseCase,
                updateCameraUploadsBackupHeartbeatStatusUseCase = updateCameraUploadsBackupHeartbeatStatusUseCase,
                isChargingUseCase = isChargingUseCase,
                monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
                broadcastStorageOverQuotaUseCase = broadcastStorageOverQuotaUseCase,
                hasMediaPermissionUseCase = hasMediaPermissionUseCase,
                cameraUploadsNotificationManagerWrapper = cameraUploadsNotificationManagerWrapper,
                cookieEnabledCheckWrapper = cookieEnabledCheckWrapper,
                broadcastCameraUploadsSettingsActionUseCase = broadcastCameraUploadsSettingsActionUseCase,
                isConnectedToInternetUseCase = isConnectedToInternetUseCase,
                loginMutex = loginMutex,
                processCameraUploadsMediaUseCase = processCameraUploadsMediaUseCase,
                getPendingCameraUploadsRecordsUseCase = getPendingCameraUploadsRecordsUseCase,
                renameCameraUploadsRecordsUseCase = renameCameraUploadsRecordsUseCase,
                uploadCameraUploadsRecordsUseCase = uploadCameraUploadsRecordsUseCase,
                doesCameraUploadsRecordExistsInTargetNodeUseCase = doesCameraUploadsRecordExistsInTargetNodeUseCase,
                extractGpsCoordinatesUseCase = extractGpsCoordinatesUseCase,
                fileSystemRepository = fileSystemRepository,
                initializeBackupsUseCase = initializeBackupsUseCase,
                areCameraUploadsFoldersInRubbishBinUseCase = areCameraUploadsFoldersInRubbishBinUseCase,
                getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
                disableCameraUploadsUseCase = disableCameraUploadsUseCase,
            )
        )
        setupMocks()
    }

    private fun setupMocks() = runBlocking {
        whenever(cameraUploadsNotificationManagerWrapper.getForegroundInfo())
            .thenReturn(foregroundInfo)
        whenever(isConnectedToInternetUseCase()).thenReturn(true)

        // mock monitor events
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorChargingStoppedState()).thenReturn(emptyFlow())
        whenever(monitorBatteryInfo()).thenReturn(emptyFlow())
        whenever(monitorPausedTransfersUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(emptyList())

        // mock check preconditions
        whenever(hasMediaPermissionUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(isNotEnoughQuota()).thenReturn(false)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(isWifiNotSatisfiedUseCase()).thenReturn(false)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryLocalPath)
        whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryNodeHandle)
        whenever(isNodeInRubbishOrDeletedUseCase(primaryNodeHandle)).thenReturn(false)
        whenever(isSecondaryFolderEnabled()).thenReturn(false)


        // mock upload process
        val tempPath = "tempPath"
        whenever(createCameraUploadTemporaryRootDirectoryUseCase()).thenReturn(tempPath)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryNodeHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryNodeHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that worker returns successfully in minimal condition`() = runTest {
        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test that the initial notification is displayed when the worker runs`() = runTest {
        underTest.doWork()

        verify(underTest).setForegroundAsync(foregroundInfo)
    }

    @Test
    fun `test that the check upload notification is displayed when the worker runs`() = runTest {
        underTest.doWork()

        verify(underTest).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that worker monitor external events when started`() = runTest {
        underTest.doWork()

        verify(monitorConnectivityUseCase).invoke()
        verify(monitorChargingStoppedState).invoke()
        verify(monitorBatteryInfo).invoke()
        verify(monitorPausedTransfersUseCase).invoke()
        verify(monitorStorageOverQuotaUseCase).invoke()
        verify(monitorNodeUpdatesUseCase).invoke()
    }

    @Test
    fun `test that worker returns failure when connection changed and wifi requirement is not satisfied`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
            whenever(isWifiNotSatisfiedUseCase()).thenReturn(true)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is rescheduled when connection changed and wifi requirement is not satisfied`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
            whenever(isWifiNotSatisfiedUseCase()).thenReturn(true)

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns failure when connection is lost`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that worker is rescheduled when when connection is lost`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))

        underTest.doWork()

        verify(scheduleCameraUploadUseCase).invoke()
    }

    @Test
    fun `test that worker returns failure when battery level too low and is not charging`() =
        runTest {
            whenever(monitorBatteryInfo()).thenReturn(flowOf(BatteryInfo(10, false)))

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is rescheduled when battery level too low and is not charging`() =
        runTest {
            whenever(monitorBatteryInfo()).thenReturn(flowOf(BatteryInfo(10, false)))

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns retry when parent node deleted`() =
        runTest {
            val nodeUpdate = NodeUpdate(
                mapOf(
                    mock<Node> {
                        on { id }.thenReturn(NodeId(primaryNodeHandle))
                    } to listOf(NodeChanges.Attributes)
                )
            )
            whenever(monitorNodeUpdatesUseCase()).thenReturn(MutableStateFlow(nodeUpdate))
            whenever(
                areCameraUploadsFoldersInRubbishBinUseCase(
                    primaryNodeHandle,
                    secondaryNodeHandle,
                    nodeUpdate
                )
            ).thenReturn(true)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        }

    @Test
    fun `test that worker returns failure when quota is not enough`() = runTest {
        whenever(isNotEnoughQuota.invoke()).thenReturn(true)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that worker is rescheduled when quota is not enough`() =
        runTest {
            whenever(isNotEnoughQuota.invoke()).thenReturn(true)

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns failure primary folder path is not valid`() =
        runTest {
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is disabled when primary folder path is not valid`() =
        runTest {
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)

            underTest.doWork()

            verify(disableCameraUploadsUseCase).invoke()
        }
}
