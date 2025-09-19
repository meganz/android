package mega.privacy.android.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.DefaultWorkerFactory
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.R
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.ARE_UPLOADS_PAUSED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_ERROR
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_SUCCESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_FILE_INDEX
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FINISHED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FINISHED_REASON
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_TYPE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_UNAVAILABLE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.OUT_OF_SPACE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.START
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_COUNT
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_TO_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOAD_BYTES
import mega.privacy.android.data.featuretoggle.DataFeatures
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.backup.InitializeBackupsUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastCameraUploadsSettingsActionUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckOrCreateCameraUploadsNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.CreateCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.DoesCameraUploadsRecordExistsInTargetNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.ExtractGpsCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPendingCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFileSizeDifferenceUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorIsChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.RenameCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.UploadCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Test class of [CameraUploadsWorker]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
internal class CameraUploadsWorkerTest {

    private lateinit var underTest: CameraUploadsWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase = mock()
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase = mock()
    private val isSecondaryFolderSetUseCase: IsSecondaryFolderSetUseCase = mock()
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase = mock()
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase = mock()
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase = mock()
    private val isChargingRequiredUseCase: IsChargingRequiredUseCase = mock()
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase = mock()
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorIsChargingRequiredToUploadContentUseCase: MonitorIsChargingRequiredToUploadContentUseCase =
        mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val handleLocalIpChangeUseCase: HandleLocalIpChangeUseCase = mock()
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase = mock()
    private val getTransferByTagUseCase: GetTransferByTagUseCase = mock()
    private val checkOrCreateCameraUploadsNodeUseCase: CheckOrCreateCameraUploadsNodeUseCase =
        mock()
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase =
        mock()
    private val disableMediaUploadSettingsUseCase: DisableMediaUploadsSettingsUseCase = mock()
    private val createCameraUploadsTemporaryRootDirectoryUseCase: CreateCameraUploadsTemporaryRootDirectoryUseCase =
        mock()
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase =
        mock()
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase = mock()

    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase =
        mock()
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase = mock()
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase =
        mock()
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase = mock()
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase = mock()
    private val cameraUploadsNotificationManagerWrapper: CameraUploadsNotificationManagerWrapper =
        mock()
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase = mock()
    private val isRootNodeExistsUseCase: RootNodeExistsUseCase = mock()
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
    private val timeSystemRepository: TimeSystemRepository = mock()
    private val initializeBackupsUseCase: InitializeBackupsUseCase = mock()
    private val areCameraUploadsFoldersInRubbishBinUseCase: AreCameraUploadsFoldersInRubbishBinUseCase =
        mock()
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase = mock()
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase = mock()
    private val getUploadFileSizeDifferenceUseCase: GetUploadFileSizeDifferenceUseCase = mock()
    private val loginMutex: Mutex = mock()
    private val crashReporter: CrashReporter = mock()
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val handleTransferEventUseCase: HandleTransferEventUseCase = mock()
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val checkEnableCameraUploadsStatusUseCase =
        mock<CheckEnableCameraUploadsStatusUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val foregroundInfo = ForegroundInfo(1, mock())
    private val primaryNodeHandle = 1111L
    private val secondaryNodeHandle = -1L
    private val primaryLocalPath = "primaryPath"
    private val tempPath = "tempPath"

    @Before
    fun setUp() {
        Dispatchers.setMain(ioDispatcher)
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(
            context,
            workExecutor.serialTaskExecutor,
            clock = SystemClock(),
            useTestDatabase = true
        )
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
                    Dispatchers.Unconfined,
                    workExecutor,
                    DefaultWorkerFactory,
                    WorkProgressUpdater(workDatabase, workExecutor),
                    WorkForegroundUpdater(
                        workDatabase,
                        { _, _ -> }, workExecutor
                    )
                ),
                isStorageOverQuotaUseCase = isStorageOverQuotaUseCase,
                getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
                isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
                isSecondaryFolderSetUseCase = isSecondaryFolderSetUseCase,
                isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
                isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
                isWifiNotSatisfiedUseCase = isWifiNotSatisfiedUseCase,
                setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
                setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
                isChargingRequiredUseCase = isChargingRequiredUseCase,
                getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
                ioDispatcher = ioDispatcher,
                monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
                monitorIsChargingRequiredToUploadContentUseCase = monitorIsChargingRequiredToUploadContentUseCase,
                backgroundFastLoginUseCase = backgroundFastLoginUseCase,
                monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
                handleLocalIpChangeUseCase = handleLocalIpChangeUseCase,
                cancelTransferByTagUseCase = cancelTransferByTagUseCase,
                getTransferByTagUseCase = getTransferByTagUseCase,
                checkOrCreateCameraUploadsNodeUseCase = checkOrCreateCameraUploadsNodeUseCase,
                establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
                disableMediaUploadSettingsUseCase = disableMediaUploadSettingsUseCase,
                createCameraUploadsTemporaryRootDirectoryUseCase = createCameraUploadsTemporaryRootDirectoryUseCase,
                deleteCameraUploadsTemporaryRootDirectoryUseCase = deleteCameraUploadsTemporaryRootDirectoryUseCase,
                scheduleCameraUploadUseCase = scheduleCameraUploadUseCase,
                updateCameraUploadsBackupStatesUseCase = updateCameraUploadsBackupStatesUseCase,
                sendBackupHeartBeatSyncUseCase = sendBackupHeartBeatSyncUseCase,
                updateCameraUploadsBackupHeartbeatStatusUseCase = updateCameraUploadsBackupHeartbeatStatusUseCase,
                monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
                broadcastStorageOverQuotaUseCase = broadcastStorageOverQuotaUseCase,
                hasMediaPermissionUseCase = hasMediaPermissionUseCase,
                cameraUploadsNotificationManagerWrapper = cameraUploadsNotificationManagerWrapper,
                isRootNodeExistsUseCase = isRootNodeExistsUseCase,
                broadcastCameraUploadsSettingsActionUseCase = broadcastCameraUploadsSettingsActionUseCase,
                isConnectedToInternetUseCase = isConnectedToInternetUseCase,
                loginMutex = loginMutex,
                processCameraUploadsMediaUseCase = processCameraUploadsMediaUseCase,
                getPendingCameraUploadsRecordsUseCase = getPendingCameraUploadsRecordsUseCase,
                renameCameraUploadsRecordsUseCase = renameCameraUploadsRecordsUseCase,
                uploadCameraUploadsRecordsUseCase = uploadCameraUploadsRecordsUseCase,
                doesCameraUploadsRecordExistsInTargetNodeUseCase = doesCameraUploadsRecordExistsInTargetNodeUseCase,
                extractGpsCoordinatesUseCase = extractGpsCoordinatesUseCase,
                timeSystemRepository = timeSystemRepository,
                initializeBackupsUseCase = initializeBackupsUseCase,
                areCameraUploadsFoldersInRubbishBinUseCase = areCameraUploadsFoldersInRubbishBinUseCase,
                getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
                disableCameraUploadsUseCase = disableCameraUploadsUseCase,
                crashReporter = crashReporter,
                monitorTransferEventsUseCase = monitorTransferEventsUseCase,
                handleTransferEventUseCase = handleTransferEventUseCase,
                correctActiveTransfersUseCase = correctActiveTransfersUseCase,
                clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
                checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
                getUploadFileSizeDifferenceUseCase = getUploadFileSizeDifferenceUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Minimal conditions for the CU to complete successfully without any uploads
     */
    private fun setupDefaultCheckConditionMocks() = runBlocking {
        whenever(cameraUploadsNotificationManagerWrapper.getForegroundInfo())
            .thenReturn(foregroundInfo)
        whenever(isConnectedToInternetUseCase()).thenReturn(true)

        // mock monitor events
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorBatteryInfoUseCase()).thenReturn(emptyFlow())
        whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(emptyFlow())
        whenever(monitorPausedTransfersUseCase()).thenReturn(emptyFlow())
        whenever(monitorTransferEventsUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())

        // mock check preconditions
        whenever(hasMediaPermissionUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(isWifiNotSatisfiedUseCase()).thenReturn(false)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryLocalPath)
        whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryNodeHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)


        // mock upload process
        whenever(createCameraUploadsTemporaryRootDirectoryUseCase()).thenReturn(tempPath)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryNodeHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryNodeHandle)
        whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(emptyList())
        whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
        )
        whenever(getUploadFileSizeDifferenceUseCase(any())).thenReturn(null)
    }

    /**
     * Minimal conditions for the CU to complete successfully without a list of files to upload
     *
     * @param list list of [CameraUploadsRecord] to upload
     */
    private fun setupDefaultProcessingFilesConditionMocks(list: List<CameraUploadsRecord>) =
        runBlocking {
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.ORIGINAL)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(list)
            whenever(
                renameCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                )
            ).thenReturn(list)
            whenever(
                doesCameraUploadsRecordExistsInTargetNodeUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                )
            ).thenReturn(list)
            whenever(extractGpsCoordinatesUseCase(list)).thenReturn(list)
        }

    @Test
    fun `test that crashReporter is invoked when the worker starts doing work`() =
        runTest {
            setupDefaultCheckConditionMocks()

            underTest.doWork()

            verify(crashReporter, times(2)).log(any())
        }

    @Test
    fun `test that the initial notification is displayed when the worker runs`() = runTest {
        setupDefaultCheckConditionMocks()

        underTest.doWork()

        verify(underTest).setForegroundAsync(foregroundInfo)
    }

    @Test
    fun `test that the worker calls handleLocalIpChangeUseCase`() = runTest {
        setupDefaultCheckConditionMocks()

        underTest.doWork()

        verify(handleLocalIpChangeUseCase).invoke(false)
    }

    @Test
    fun `test that the worker cancel all error notifications after preconditions passed`() =
        runTest {
            setupDefaultCheckConditionMocks()

            underTest.doWork()

            verify(underTest).setProgress(workDataOf(STATUS_INFO to START))
        }

    @Test
    fun `test that the check files to upload notification is displayed after preconditions passed`() =
        runTest {
            setupDefaultCheckConditionMocks()

            underTest.doWork()

            verify(underTest).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that all processing of the files before upload are done`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val list = listOf<CameraUploadsRecord>(mock())
            setupDefaultProcessingFilesConditionMocks(list)

            underTest.doWork()

            val inOrder = inOrder(
                processCameraUploadsMediaUseCase,
                getPendingCameraUploadsRecordsUseCase,
                doesCameraUploadsRecordExistsInTargetNodeUseCase,
                renameCameraUploadsRecordsUseCase,
                extractGpsCoordinatesUseCase,
            )

            inOrder.verify(processCameraUploadsMediaUseCase).invoke(tempPath)
            inOrder.verify(getPendingCameraUploadsRecordsUseCase).invoke()
            inOrder.verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(list, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
            inOrder.verify(renameCameraUploadsRecordsUseCase)
                .invoke(list, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
            inOrder.verify(extractGpsCoordinatesUseCase).invoke(list)
        }

    @Test
    fun `test that the processing of the files to upload are not done if the list to upload is empty`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(emptyList())

            underTest.doWork()

            val inOrder = inOrder(
                processCameraUploadsMediaUseCase,
                getPendingCameraUploadsRecordsUseCase,
                renameCameraUploadsRecordsUseCase,
                doesCameraUploadsRecordExistsInTargetNodeUseCase,
                extractGpsCoordinatesUseCase,
            )

            inOrder.verify(processCameraUploadsMediaUseCase).invoke(tempPath)
            inOrder.verify(getPendingCameraUploadsRecordsUseCase).invoke()
            inOrder.verify(renameCameraUploadsRecordsUseCase, never())
                .invoke(any(), eq(NodeId(primaryNodeHandle)), eq(NodeId(secondaryNodeHandle)))
            inOrder.verify(doesCameraUploadsRecordExistsInTargetNodeUseCase, never())
                .invoke(any(), eq(NodeId(primaryNodeHandle)), eq(NodeId(secondaryNodeHandle)))
            inOrder.verify(extractGpsCoordinatesUseCase, never()).invoke(any())
        }

    @Test
    fun `test that the initial backup state is sent with value ACTIVE if the transfers are not paused when uploads starts`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(false))
            val list = listOf<CameraUploadsRecord>(mock())
            setupDefaultProcessingFilesConditionMocks(list)

            underTest.doWork()

            verify(monitorPausedTransfersUseCase, atLeastOnce()).invoke()
            verify(updateCameraUploadsBackupStatesUseCase).invoke(BackupState.ACTIVE)
        }

    @Test
    fun `test that the initial backup state is sent with value PAUSE_UPLOADS if the transfers are paused when uploads starts`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(true))
            val list = listOf<CameraUploadsRecord>(mock())
            setupDefaultProcessingFilesConditionMocks(list)

            underTest.doWork()

            verify(monitorPausedTransfersUseCase, atLeastOnce()).invoke()
            verify(updateCameraUploadsBackupStatesUseCase).invoke(BackupState.PAUSE_UPLOADS)
        }

    @Test
    fun `test that the backup heartbeat is sent regularly when uploads occurs`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(true))
        val list = listOf<CameraUploadsRecord>(mock())
        setupDefaultProcessingFilesConditionMocks(list)

        underTest.doWork()

        verify(sendBackupHeartBeatSyncUseCase).invoke(any())
    }

    @Test
    fun `test that the uploads occurs after the processing of the files to upload`() = runTest {
        setupDefaultCheckConditionMocks()
        val list = listOf<CameraUploadsRecord>(mock())
        setupDefaultProcessingFilesConditionMocks(list)

        underTest.doWork()

        verify(uploadCameraUploadsRecordsUseCase)
            .invoke(list, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle), tempPath)
    }

    @Test
    fun `test that the state is updated and emitted properly when an event ToUpload, UploadInProgress TransferUpdate, and Uploaded are received`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val toUploadEvent = CameraUploadsTransferProgress.ToUpload(
                record = record,
                transferEvent = TransferEvent.TransferStartEvent(transfer = mock()),
            )
            val transferUpdateEvent = CameraUploadsTransferProgress.UploadInProgress.TransferUpdate(
                record = record,
                transferEvent = TransferEvent.TransferUpdateEvent(transfer = mock {
                    on { isFinished }.thenReturn(false)
                    on { transferredBytes }.thenReturn(size / 2 * 1024 * 1024)
                }),
            )
            val uploadedEvent = CameraUploadsTransferProgress.Uploaded(
                record = record,
                transferEvent =
                    TransferEvent.TransferFinishEvent(
                        transfer = mock {
                            on { isFinished }.thenReturn(true)
                            on { transferredBytes }.thenReturn(size * 1024 * 1024)
                        },
                        error = null
                    ),
                nodeId = mock(),
            )
            val flow = flow {
                emit(toUploadEvent)
                emit(transferUpdateEvent)
                emit(uploadedEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)
            val currentTime = 10000L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(
                currentTime,
                currentTime + 1000,
                currentTime + 2000,
            )

            val afterToUploadEventData = workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to 0,
                TOTAL_TO_UPLOAD to 1,
                TOTAL_UPLOADED_BYTES to 0L,
                TOTAL_UPLOAD_BYTES to size * 1024 * 1024,
                CURRENT_PROGRESS to 0f,
                ARE_UPLOADS_PAUSED to false,
            )
            val afterTransferUpdateEventData = workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to 0,
                TOTAL_TO_UPLOAD to 1,
                TOTAL_UPLOADED_BYTES to size / 2 * 1024 * 1024,
                TOTAL_UPLOAD_BYTES to size * 1024 * 1024,
                CURRENT_PROGRESS to 0.5f,
                ARE_UPLOADS_PAUSED to false,
            )
            val afterUploadedEventData = workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to 1,
                TOTAL_TO_UPLOAD to 1,
                TOTAL_UPLOADED_BYTES to size * 1024 * 1024,
                TOTAL_UPLOAD_BYTES to size * 1024 * 1024,
                CURRENT_PROGRESS to 1f,
                ARE_UPLOADS_PAUSED to false,
            )
            underTest.doWork()
            verify(underTest).setProgress(afterToUploadEventData)
            verify(underTest).setProgress(afterTransferUpdateEventData)
            verify(underTest).setProgress(afterUploadedEventData)
        }

    @Test
    fun `test that the worker is broadcasting quota exceeded event when an event UploadInProgress TransferTemporaryError is received with error QuotaExceededMegaException`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val transferTemporaryErrorEvent =
                CameraUploadsTransferProgress.UploadInProgress.TransferTemporaryError(
                    record = record,
                    transferEvent =
                        TransferEvent.TransferTemporaryErrorEvent(
                            transfer = mock {},
                            error = mock<QuotaExceededMegaException>()
                        ),
                )
            val flow = flow {
                emit(transferTemporaryErrorEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)

            underTest.doWork()
            verify(broadcastStorageOverQuotaUseCase).invoke()
        }

    @Test
    fun `test that the worker is broadcasting quota exceeded event when an event Uploaded is received with error QuotaExceededMegaException`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val uploadedEvent = CameraUploadsTransferProgress.Uploaded(
                record = record,
                transferEvent =
                    TransferEvent.TransferFinishEvent(
                        transfer = mock {
                            on { isFinished }.thenReturn(true)
                            on { transferredBytes }.thenReturn(size * 1024 * 1024)
                        },
                        error = mock<QuotaExceededMegaException>()
                    ),
                nodeId = mock(),
            )
            val flow = flow {
                emit(uploadedEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)

            underTest.doWork()
            verify(broadcastStorageOverQuotaUseCase).invoke()
        }

    @Test
    fun `test that the state is updated and emitted properly when an event ToCopy and Copied is received`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val toCopyEvent = CameraUploadsTransferProgress.ToCopy(
                record = record,
                nodeId = mock()
            )
            val copiedEvent = CameraUploadsTransferProgress.Copied(
                record = record,
                nodeId = mock(),
            )
            val flow = flow {
                emit(toCopyEvent)
                emit(copiedEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)
            val currentTime = 10000L
            whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(
                currentTime,
                currentTime + 1000
            )

            val afterToCopyEventData = workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to 0,
                TOTAL_TO_UPLOAD to 1,
                TOTAL_UPLOADED_BYTES to 0L,
                TOTAL_UPLOAD_BYTES to size * 1024 * 1024,
                CURRENT_PROGRESS to 0f,
                ARE_UPLOADS_PAUSED to false,
            )
            val afterCopiedEventData = workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to 1,
                TOTAL_TO_UPLOAD to 1,
                TOTAL_UPLOADED_BYTES to size * 1024 * 1024,
                TOTAL_UPLOAD_BYTES to size * 1024 * 1024,
                CURRENT_PROGRESS to 1f,
                ARE_UPLOADS_PAUSED to false,
            )
            underTest.doWork()
            verify(underTest).setProgress(afterToCopyEventData)
            verify(underTest).setProgress(afterCopiedEventData)
        }

    @Test
    fun `test that the compression progress is emitted properly when an event Compressing Progress and Compressing Successful is received`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val progress = 0.5f
            val progress2 = progress + 0.2f
            val compressionProgressEvent = CameraUploadsTransferProgress.Compressing.Progress(
                record = record,
                progress = progress,
            )
            val compressionProgressEvent2 = CameraUploadsTransferProgress.Compressing.Progress(
                record = record,
                progress = progress2,
            )
            val compressionSuccessfulEvent = CameraUploadsTransferProgress.Compressing.Successful(
                record = record,
            )
            val flow = flow {
                emit(compressionProgressEvent)
                emit(compressionProgressEvent2)
                emit(compressionSuccessfulEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)
            val afterCompressionProgressEventData = workDataOf(
                STATUS_INFO to COMPRESSION_PROGRESS,
                CURRENT_PROGRESS to progress,
                CURRENT_FILE_INDEX to 1,
                TOTAL_COUNT to 1,
            )
            val afterCompressionProgressEventData2 = workDataOf(
                STATUS_INFO to COMPRESSION_PROGRESS,
                CURRENT_PROGRESS to progress2,
                CURRENT_FILE_INDEX to 1,
                TOTAL_COUNT to 1,
            )
            underTest.doWork()
            verify(underTest).setProgress(afterCompressionProgressEventData)
            verify(underTest).setProgress(afterCompressionProgressEventData2)
            verify(underTest).setProgress(workDataOf(STATUS_INFO to COMPRESSION_SUCCESS))
        }

    @Test
    fun `test that the worker returns failure and an error notification is displayed when an event Compressing InsufficientStorage is received`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val insufficientStorageEvent =
                CameraUploadsTransferProgress.Compressing.InsufficientStorage(
                    record = record,
                )
            val flow = flow {
                emit(insufficientStorageEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)
            val afterInsufficientStorageEventData = workDataOf(
                STATUS_INFO to OUT_OF_SPACE,
            )
            val result = underTest.doWork()
            verify(underTest).setProgress(afterInsufficientStorageEventData)
            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that an error notification is displayed when an event Compressing Cancel is received`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val cancelEvent =
                CameraUploadsTransferProgress.Compressing.Cancel(
                    record = record,
                )
            val flow = flow {
                emit(cancelEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)
            val afterInsufficientStorageEventData = workDataOf(
                STATUS_INFO to COMPRESSION_ERROR,
            )
            underTest.doWork()
            verify(underTest).setProgress(afterInsufficientStorageEventData)
        }

    @Test
    fun `test that the worker returns failure and an error notification is displayed when an event Error is received with error NotEnoughStorageException`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val errorEvent = CameraUploadsTransferProgress.Error(
                record = record,
                error = mock<NotEnoughStorageException>(),
            )
            val flow = flow {
                emit(errorEvent)
            }
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)

            val afterErrorEventData = workDataOf(
                STATUS_INFO to CameraUploadsWorkerStatusConstant.NOT_ENOUGH_STORAGE,
            )
            val result = underTest.doWork()
            verify(underTest).setProgress(afterErrorEventData)
            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that video files are not filtered out if video quality is original`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val photoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
            }
            val videoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
            }
            val expected = listOf(photoRecord, videoRecord)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(expected)
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.ORIGINAL)

            underTest.doWork()

            verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(expected, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
        }

    @Test
    fun `test that video files are filtered out and error notification is shown if video quality is not original and video compression requirement are not met and device not charging`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val photoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
                on { filePath }.thenReturn("photoRecordPath")
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val videoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { filePath }.thenReturn("videoRecordPath")
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }

            val list = listOf(photoRecord, videoRecord)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(list)
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.LOW)
            whenever(isChargingRequiredUseCase(size)).thenReturn(true)
            whenever(monitorBatteryInfoUseCase()).thenReturn(
                flowOf(BatteryInfo(100, false))
            )
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(false))

            underTest.doWork()

            val expected = list.filter { it.type == CameraUploadsRecordType.TYPE_PHOTO }
            verify(underTest).setProgress(workDataOf(STATUS_INFO to COMPRESSION_ERROR))
            verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(expected, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
        }

    @Test
    fun `test that video files are not filtered out if video quality is not original and video compression requirement are not met and device charging`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val size = 2L // in MB
            val photoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
                on { filePath }.thenReturn("photoRecordPath")
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val videoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { filePath }.thenReturn("videoRecordPath")
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }

            val expected = listOf(photoRecord, videoRecord)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(expected)
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.LOW)
            whenever(isChargingRequiredUseCase(size)).thenReturn(true)
            whenever(monitorBatteryInfoUseCase()).thenReturn(
                flowOf(BatteryInfo(100, true))
            )
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(false))

            underTest.doWork()

            verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(expected, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
        }

    @Test
    fun `test that if the list of files to upload is empty, then no upload is triggered`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val list = emptyList<CameraUploadsRecord>()
            setupDefaultProcessingFilesConditionMocks(list)

            underTest.doWork()

            verify(updateCameraUploadsBackupStatesUseCase, never()).invoke(any())
            verify(sendBackupHeartBeatSyncUseCase, never()).invoke(any())
            verify(uploadCameraUploadsRecordsUseCase, never()).invoke(
                any(),
                eq(NodeId(primaryNodeHandle)),
                eq(NodeId(secondaryNodeHandle)),
                eq(tempPath)
            )
        }

    @Test
    fun `test that the worker returns failure when camera uploads is not enabled`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)

        val result = underTest.doWork()

        verify(underTest).setProgress(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.DISABLED.name
            )
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when required permissions are not granted`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when login is not successful`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        verify(underTest).setProgress(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.LOGIN_FAILED.name
            )
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure, reset primary local folder paths and show an error notification when local primary folder path is not valid`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(false)

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(setPrimaryFolderLocalPathUseCase).invoke("")
            verify(broadcastCameraUploadsSettingsActionUseCase).invoke(CameraUploadsSettingsAction.DisableCameraUploads)
            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FOLDER_UNAVAILABLE,
                    FOLDER_TYPE to CameraUploadFolderType.Primary.ordinal
                )
            )
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker disable media uploads, reset secondary local folder path and show an error notification when media uploads enabled and local secondary folder path is not valid`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(isSecondaryFolderSetUseCase()).thenReturn(false)

            underTest.doWork()

            verify(disableMediaUploadSettingsUseCase).invoke()
            verify(setSecondaryFolderLocalPathUseCase).invoke("")
            verify(broadcastCameraUploadsSettingsActionUseCase).invoke(CameraUploadsSettingsAction.DisableMediaUploads)
            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FOLDER_UNAVAILABLE,
                    FOLDER_TYPE to CameraUploadFolderType.Secondary.ordinal
                )
            )
        }

    @Test
    fun `test that the worker returns failure when upload nodes are not retrieved`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(establishCameraUploadsSyncHandlesUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        verify(underTest).setProgress(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.ERROR_DURING_PROCESS.name
            )
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when primary upload node is not retrieved and fails to be created`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(
                checkOrCreateCameraUploadsNodeUseCase(
                    context.getString(R.string.section_photo_sync),
                    CameraUploadFolderType.Primary
                )
            ).thenThrow(RuntimeException())

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.ERROR_DURING_PROCESS.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when secondary upload node is not retrieved and fails to be created`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(isSecondaryFolderSetUseCase()).thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
                .thenReturn(-1L)
            whenever(
                checkOrCreateCameraUploadsNodeUseCase(
                    context.getString(R.string.section_secondary_media_uploads),
                    CameraUploadFolderType.Secondary
                )
            ).thenThrow(RuntimeException())

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.ERROR_DURING_PROCESS.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure is backup not initialized`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(initializeBackupsUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when it fails to create the temporary folder`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(createCameraUploadsTemporaryRootDirectoryUseCase()).thenThrow(RuntimeException())

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.ERROR_DURING_PROCESS.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the check upload notification is displayed when the worker runs`() = runTest {
        setupDefaultCheckConditionMocks()

        underTest.doWork()

        verify(underTest).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that worker monitor external events when started`() = runTest {
        setupDefaultCheckConditionMocks()

        underTest.doWork()

        verify(monitorConnectivityUseCase).invoke()
        verify(monitorBatteryInfoUseCase).invoke()
        verify(monitorIsChargingRequiredToUploadContentUseCase).invoke()
        verify(monitorStorageOverQuotaUseCase).invoke()
        verify(monitorNodeUpdatesUseCase).invoke()
    }

    @Test
    fun `test that worker returns failure when connection changed and wifi requirement is not satisfied`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
            whenever(isWifiNotSatisfiedUseCase()).thenReturn(true)
            whenever(
                getFeatureFlagValueUseCase(DataFeatures.CameraUploadsNotification)
            ).thenReturn(true)

            val afterErrorEventData = workDataOf(
                STATUS_INFO to CameraUploadsWorkerStatusConstant.NO_WIFI_CONNECTION,
            )

            val result = underTest.doWork()

            verify(underTest).setProgress(afterErrorEventData)
            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is rescheduled when connection changed and wifi requirement is not satisfied`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
            whenever(isWifiNotSatisfiedUseCase()).thenReturn(true)

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns failure when connection is lost`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        whenever(isWifiNotSatisfiedUseCase()).thenReturn(false)
        whenever(
            getFeatureFlagValueUseCase(DataFeatures.CameraUploadsNotification)
        ).thenReturn(true)
        val afterErrorEventData = workDataOf(
            STATUS_INFO to CameraUploadsWorkerStatusConstant.NO_NETWORK_CONNECTION,
        )

        val result = underTest.doWork()

        verify(underTest).setProgress(afterErrorEventData)
        verify(underTest).setProgress(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET.name
            )
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that worker is rescheduled when when connection is lost`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))

        underTest.doWork()

        verify(scheduleCameraUploadUseCase).invoke()
    }

    @Test
    fun `test that only camera uploads transfer events are being handled`() = runTest {
        setupDefaultCheckConditionMocks()
        val cameraUploadsTransferEvent = TransferEvent.TransferStartEvent(
            mock<Transfer> { on { transferType }.thenReturn(TransferType.CU_UPLOAD) }
        )
        val chatTransferEvent = TransferEvent.TransferStartEvent(
            mock<Transfer> { on { transferType }.thenReturn(TransferType.CHAT_UPLOAD) }
        )
        val generalUploadTransferEvent = TransferEvent.TransferStartEvent(
            mock<Transfer> { on { transferType }.thenReturn(TransferType.GENERAL_UPLOAD) }
        )
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(cameraUploadsTransferEvent, chatTransferEvent, generalUploadTransferEvent)
        )

        underTest.doWork()

        verify(handleTransferEventUseCase).invoke(events = listOf(cameraUploadsTransferEvent).toTypedArray())
    }

    @Test
    fun `test that no camera uploads transfer events are not handled`() = runTest {
        setupDefaultCheckConditionMocks()
        val chatTransferEvent = TransferEvent.TransferStartEvent(
            mock<Transfer> { on { transferType }.thenReturn(TransferType.CHAT_UPLOAD) }
        )
        val generalUploadTransferEvent = TransferEvent.TransferStartEvent(
            mock<Transfer> { on { transferType }.thenReturn(TransferType.GENERAL_UPLOAD) }
        )
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(chatTransferEvent, generalUploadTransferEvent)
        )

        underTest.doWork()

        verifyNoInteractions(handleTransferEventUseCase)
    }

    @Ignore("This Test is flaky as it sometimes fails in the Gitlab pipeline. Check the test implementation again")
    @Test
    fun `test that the worker returns failure when the battery level is too low and is not charging`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(10, false)))
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(false))

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Ignore("This Test is flaky as it sometimes fails in the Gitlab pipeline. Check the test implementation again")
    @Test
    fun `test that the worker is rescheduled when the battery level is too low and is not charging`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(10, false)))
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(false))

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Ignore("This Test is flaky as it sometimes fails in the Gitlab pipeline. Check the test implementation again")
    @Test
    fun `test that the worker returns failure when the charging requirement is not satisfied`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(100, false)))
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(true))

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET.name,
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that the worker is rescheduled when the charging requirement is not satisfied`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(100, false)))
            whenever(monitorIsChargingRequiredToUploadContentUseCase()).thenReturn(flowOf(true))

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns retry when parent node deleted`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val nodeUpdate = NodeUpdate(
                mapOf(
                    mock<Node> {
                        on { id }.thenReturn(NodeId(primaryNodeHandle))
                    } to listOf(NodeChanges.Attributes)
                )
            )
            whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(nodeUpdate))
            whenever(
                areCameraUploadsFoldersInRubbishBinUseCase(
                    primaryNodeHandle,
                    secondaryNodeHandle,
                    nodeUpdate
                )
            ).thenReturn(true)

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.TARGET_NODES_DELETED.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        }

    @Test
    fun `test that worker returns failure when quota is not enough`() = runTest {
        setupDefaultCheckConditionMocks()
        whenever(isStorageOverQuotaUseCase.invoke()).thenReturn(true)

        val result = underTest.doWork()

        verify(underTest).setProgress(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA.name
            )
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that worker returns success when the camera uploads status is SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(checkEnableCameraUploadsStatusUseCase.invoke()).thenReturn(
                EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
            )

            val result = underTest.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that worker returns failure when the camera uploads status is SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(checkEnableCameraUploadsStatusUseCase.invoke()).thenReturn(
                EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
            )

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.BUSINESS_ACCOUNT_EXPIRED.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker returns failure when the camera uploads status is SHOW_SUSPENDED_PRO_FLEXI_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(checkEnableCameraUploadsStatusUseCase.invoke()).thenReturn(
                EnableCameraUploadsStatus.SHOW_SUSPENDED_PRO_FLEXI_BUSINESS_ACCOUNT_PROMPT
            )

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.BUSINESS_ACCOUNT_EXPIRED.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker returns failure when the camera uploads status is SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(checkEnableCameraUploadsStatusUseCase.invoke()).thenReturn(
                EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT
            )

            val result = underTest.doWork()

            verify(underTest).setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    FINISHED_REASON to CameraUploadsFinishedReason.BUSINESS_ACCOUNT_EXPIRED.name
                )
            )
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is rescheduled when quota is not enough`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isStorageOverQuotaUseCase.invoke()).thenReturn(true)

            underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that worker returns failure primary local folder path is not valid`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that worker is disabled when primary folder path is not valid`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)

            underTest.doWork()

            verify(disableCameraUploadsUseCase).invoke()
        }

    @Test
    fun `test that the worker is disabled when required permissions are not granted`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            underTest.doWork()

            verify(disableCameraUploadsUseCase).invoke()
        }

    @Test
    fun `test that the CU is scheduled when the worker complete with success`() =
        runTest {
            setupDefaultCheckConditionMocks()

            val result = underTest.doWork()

            verify(scheduleCameraUploadUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the temporary folder is deleted when the worker complete with success`() =
        runTest {
            setupDefaultCheckConditionMocks()

            val result = underTest.doWork()

            verify(deleteCameraUploadsTemporaryRootDirectoryUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that all transfers are cancelled when the worker complete with failure`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val fakeFlow = MutableStateFlow(true)
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val uploadTag = 100
            val transfer = mock<Transfer> {
                on { tag }.thenReturn(uploadTag)
                on { isFinished }.thenReturn(false)
            }
            val toUploadEvent = CameraUploadsTransferProgress.ToUpload(
                record = record,
                transferEvent = TransferEvent.TransferStartEvent(transfer = transfer),
            )
            val flow = channelFlow {
                send(toUploadEvent)
                fakeFlow.emit(false)
            }
            whenever(monitorConnectivityUseCase()).thenReturn(fakeFlow)
            whenever(getTransferByTagUseCase(uploadTag)).thenReturn(transfer)
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)

            underTest.doWork()

            verify(cancelTransferByTagUseCase).invoke(uploadTag)
        }

    @Test
    fun `test that no child jobs are running when the worker complete`() =
        runTest {
            setupDefaultCheckConditionMocks()

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the interrupted backup is sent when the worker complete with failure`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            val result = underTest.doWork()

            verify(updateCameraUploadsBackupStatesUseCase).invoke(BackupState.TEMPORARILY_DISABLED)
            verify(updateCameraUploadsBackupHeartbeatStatusUseCase)
                .invoke(eq(HeartbeatStatus.INACTIVE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that the up-to-date backup state is sent when the worker complete with success`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            val result = underTest.doWork()

            verify(updateCameraUploadsBackupHeartbeatStatusUseCase)
                .invoke(eq(HeartbeatStatus.UP_TO_DATE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the interrupted backup is not sent when the worker complete with failure and no internet connection`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)
            whenever(isConnectedToInternetUseCase()).thenReturn(false)

            val result = underTest.doWork()

            verify(updateCameraUploadsBackupStatesUseCase, never())
                .invoke(BackupState.TEMPORARILY_DISABLED)
            verify(updateCameraUploadsBackupHeartbeatStatusUseCase, never())
                .invoke(eq(HeartbeatStatus.INACTIVE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that the up-to-date backup state is not sent when the worker complete with success and no internet connection`() =
        runTest {
            setupDefaultCheckConditionMocks()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)

            val result = underTest.doWork()

            verify(updateCameraUploadsBackupHeartbeatStatusUseCase, never())
                .invoke(eq(HeartbeatStatus.UP_TO_DATE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that correctActiveTransfersUseCase is invoked when worker starts work`() = runTest {
        setupDefaultCheckConditionMocks()

        underTest.doWork()

        verify(correctActiveTransfersUseCase).invoke(TransferType.CU_UPLOAD)
    }

    @Test
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when the worker complete with success`() =
        runTest {
            setupDefaultCheckConditionMocks()

            val result = underTest.doWork()

            verify(clearActiveTransfersIfFinishedUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when the worker complete with failure`() =
        runTest {
            setupDefaultCheckConditionMocks()
            val fakeFlow = MutableStateFlow(true)
            val size = 2L // in MB
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { fileSize }.thenReturn(size * 1024 * 1024)
            }
            val list = listOf(record)
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val uploadTag = 100
            val transfer = mock<Transfer> {
                on { tag }.thenReturn(uploadTag)
                on { isFinished }.thenReturn(false)
            }
            val toUploadEvent = CameraUploadsTransferProgress.ToUpload(
                record = record,
                transferEvent = TransferEvent.TransferStartEvent(transfer = transfer),
            )
            val flow = channelFlow {
                send(toUploadEvent)
                fakeFlow.emit(false)
            }
            whenever(monitorConnectivityUseCase()).thenReturn(fakeFlow)
            whenever(getTransferByTagUseCase(uploadTag)).thenReturn(transfer)
            whenever(
                uploadCameraUploadsRecordsUseCase(
                    list,
                    NodeId(primaryNodeHandle),
                    NodeId(secondaryNodeHandle),
                    tempPath
                )
            ).thenReturn(flow)

            underTest.doWork()

            verify(clearActiveTransfersIfFinishedUseCase).invoke()
        }
}
