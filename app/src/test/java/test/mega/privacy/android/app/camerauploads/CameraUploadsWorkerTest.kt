package test.mega.privacy.android.app.camerauploads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import mega.privacy.android.data.worker.CameraUploadsWorker
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
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
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
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
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
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
    private val timeSystemRepository: TimeSystemRepository = mock()
    private val initializeBackupsUseCase: InitializeBackupsUseCase = mock()
    private val areCameraUploadsFoldersInRubbishBinUseCase: AreCameraUploadsFoldersInRubbishBinUseCase =
        mock()
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase = mock()
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase = mock()
    private val getFileByPathUseCase: GetFileByPathUseCase = mock()
    private val loginMutex: Mutex = mock()

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
                    workExecutor,
                    WorkerFactory.getDefaultWorkerFactory(),
                    WorkProgressUpdater(workDatabase, workExecutor),
                    WorkForegroundUpdater(
                        workDatabase,
                        { _, _ -> }, workExecutor
                    )
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
                timeSystemRepository = timeSystemRepository,
                initializeBackupsUseCase = initializeBackupsUseCase,
                areCameraUploadsFoldersInRubbishBinUseCase = areCameraUploadsFoldersInRubbishBinUseCase,
                getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
                disableCameraUploadsUseCase = disableCameraUploadsUseCase,
                getFileByPathUseCase = getFileByPathUseCase,
            )
        )
        setupDefaultCheckConditionMocks()
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
        whenever(createCameraUploadTemporaryRootDirectoryUseCase()).thenReturn(tempPath)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryNodeHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryNodeHandle)
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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial notification is displayed when the worker runs`() = runTest {
        underTest.doWork()

        verify(underTest).setForegroundAsync(foregroundInfo)
    }

    @Test
    fun `test that the worker calls handleLocalIpChangeUseCase`() = runTest {
        underTest.doWork()

        verify(handleLocalIpChangeUseCase).invoke(false)
    }

    @Test
    fun `test that the worker cancel all error notifications after preconditions passed`() =
        runTest {
            underTest.doWork()

            verify(underTest).setProgress(workDataOf(STATUS_INFO to START))
        }

    @Test
    fun `test that the check files to upload notification is displayed after preconditions passed`() =
        runTest {
            underTest.doWork()

            verify(underTest).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that all processing of the files before upload are done`() =
        runTest {
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
            whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(true))
            val list = listOf<CameraUploadsRecord>(mock())
            setupDefaultProcessingFilesConditionMocks(list)

            underTest.doWork()

            verify(monitorPausedTransfersUseCase, atLeastOnce()).invoke()
            verify(updateCameraUploadsBackupStatesUseCase).invoke(BackupState.PAUSE_UPLOADS)
        }

    @Test
    fun `test that the backup heartbeat is sent regularly when uploads occurs`() = runTest {
        whenever(monitorPausedTransfersUseCase()).thenReturn(flowOf(true))
        val list = listOf<CameraUploadsRecord>(mock())
        setupDefaultProcessingFilesConditionMocks(list)

        underTest.doWork()

        verify(sendBackupHeartBeatSyncUseCase).invoke(any())
    }

    @Test
    fun `test that the uploads occurs after the processing of the files to upload`() = runTest {
        val list = listOf<CameraUploadsRecord>(mock())
        setupDefaultProcessingFilesConditionMocks(list)

        underTest.doWork()

        verify(uploadCameraUploadsRecordsUseCase)
            .invoke(list, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle), tempPath)
    }

    @Test
    fun `test that the state is updated and emitted properly when an event ToUpload, UploadInProgress TransferUpdate, and Uploaded are received`() =
        runTest {
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that an error notification is displayed when an event Compressing Cancel is received`() =
        runTest {
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            val record = mock<CameraUploadsRecord> {
                on { folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            val list = listOf(record)
            val size = 2L // in MB
            val file = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }
            setupDefaultProcessingFilesConditionMocks(list)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(getFileByPathUseCase(record.filePath)).thenReturn(file)

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
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that video files are not filtered out if video quality is original`() =
        runTest {
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
            val photoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
                on { filePath }.thenReturn("photoRecordPath")
            }
            val videoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { filePath }.thenReturn("videoRecordPath")
            }
            val size = 2L // in MB
            val videoFile = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }

            val list = listOf(photoRecord, videoRecord)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(list)
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.LOW)
            whenever(getFileByPathUseCase(videoRecord.filePath)).thenReturn(videoFile)
            whenever(isChargingRequired(size)).thenReturn(true)
            whenever(isChargingUseCase()).thenReturn(false)

            underTest.doWork()

            val expected = list.filter { it.type == CameraUploadsRecordType.TYPE_PHOTO }
            verify(underTest).setProgress(workDataOf(STATUS_INFO to COMPRESSION_ERROR))
            verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(expected, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
        }

    @Test
    fun `test that video files are not filtered out if video quality is not original and video compression requirement are not met and device charging`() =
        runTest {
            val photoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
                on { filePath }.thenReturn("photoRecordPath")
            }
            val videoRecord = mock<CameraUploadsRecord> {
                on { type }.thenReturn(CameraUploadsRecordType.TYPE_VIDEO)
                on { filePath }.thenReturn("videoRecordPath")
            }
            val size = 2L // in MB
            val videoFile = mock<File> {
                on { length() }.thenReturn(size * 1024 * 1024)
            }

            val expected = listOf(photoRecord, videoRecord)
            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(expected)
            whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.LOW)
            whenever(getFileByPathUseCase(videoRecord.filePath)).thenReturn(videoFile)
            whenever(isChargingRequired(size)).thenReturn(true)
            whenever(isChargingUseCase()).thenReturn(true)

            underTest.doWork()

            verify(doesCameraUploadsRecordExistsInTargetNodeUseCase)
                .invoke(expected, NodeId(primaryNodeHandle), NodeId(secondaryNodeHandle))
        }

    @Test
    fun `test that if the list of files to upload is empty, then no upload is triggered`() =
        runTest {
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
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when required permissions are not granted`() =
        runTest {
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when login is not successful`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure, reset primary local folder paths and show an error notification when local primary folder path is not valid`() =
        runTest {
            whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(false)

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(setPrimaryFolderLocalPathUseCase).invoke("")
            verify(broadcastCameraUploadsSettingsActionUseCase).invoke(CameraUploadsSettingsAction.RefreshSettings)
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
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(isSecondaryFolderSetUseCase()).thenReturn(false)

            underTest.doWork()

            verify(disableMediaUploadSettings).invoke()
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
        whenever(establishCameraUploadsSyncHandlesUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when primary upload node does not exist and fails to be created`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(-1L)
            whenever(getDefaultNodeHandleUseCase(context.getString(R.string.section_photo_sync)))
                .thenReturn(-1L)
            whenever(createCameraUploadFolder(context.getString(R.string.section_photo_sync)))
                .thenThrow(RuntimeException())

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when secondary upload node does not exist and fails to be created`() =
        runTest {
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(isSecondaryFolderSetUseCase()).thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
                .thenReturn(-1L)
            whenever(getDefaultNodeHandleUseCase(context.getString(R.string.section_secondary_media_uploads)))
                .thenReturn(-1L)
            whenever(createCameraUploadFolder(context.getString(R.string.section_secondary_media_uploads)))
                .thenThrow(RuntimeException())

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when primary upload node is in rubbish bin and fails to be created`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(1111L)
            whenever(isNodeInRubbishOrDeletedUseCase(1111L)).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(context.getString(R.string.section_photo_sync)))
                .thenReturn(1111L)
            whenever(createCameraUploadFolder(context.getString(R.string.section_photo_sync)))
                .thenThrow(RuntimeException())

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure when secondary upload node is in rubbish bin and fails to be created`() =
        runTest {
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(isSecondaryFolderSetUseCase()).thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
                .thenReturn(2222L)
            whenever(isNodeInRubbishOrDeletedUseCase(2222L)).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(context.getString(R.string.section_secondary_media_uploads)))
                .thenReturn(2222L)
            whenever(createCameraUploadFolder(context.getString(R.string.section_secondary_media_uploads)))
                .thenThrow(RuntimeException())

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }

    @Test
    fun `test that the worker returns failure is backup not initialized`() = runTest {
        whenever(initializeBackupsUseCase()).thenThrow(RuntimeException())

        val result = underTest.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    @Test
    fun `test that the worker returns failure when it fails to create the temporary folder`() =
        runTest {
            whenever(createCameraUploadTemporaryRootDirectoryUseCase()).thenThrow(RuntimeException())

            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
            verify(underTest, never()).setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
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
        verify(monitorBatteryInfo).invoke()
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
            whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(nodeUpdate))
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
    fun `test that worker returns failure primary local folder path is not valid`() =
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

    @Test
    fun `test that the worker is disabled when required permissions are not granted`() =
        runTest {
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            underTest.doWork()

            verify(disableCameraUploadsUseCase).invoke()
        }

    @Test
    fun `test that the temporary folder is deleted when the worker complete with success`() =
        runTest {
            val result = underTest.doWork()
            verify(deleteCameraUploadsTemporaryRootDirectoryUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that total upload count is reset on the back end when the worker complete with success`() =
        runTest {
            val result = underTest.doWork()
            verify(resetTotalUploadsUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that all pending transfers are cancelled when the worker complete with failure`() =
        runTest {
            whenever(isPrimaryFolderPathValidUseCase(primaryLocalPath)).thenReturn(false)
            val result = underTest.doWork()
            verify(cancelAllUploadTransfersUseCase).invoke()
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that no child jobs are running when the worker complete`() =
        runTest {
            val result = underTest.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the interrupted backup is sent when the worker complete with failure`() =
        runTest {
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
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            val result = underTest.doWork()
            verify(updateCameraUploadsBackupHeartbeatStatusUseCase)
                .invoke(eq(HeartbeatStatus.UP_TO_DATE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `test that the interrupted backup is not sent when the worker complete with failure and no internet connection`() =
        runTest {
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
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            val result = underTest.doWork()
            verify(updateCameraUploadsBackupHeartbeatStatusUseCase, never())
                .invoke(eq(HeartbeatStatus.UP_TO_DATE), any())
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }
}
