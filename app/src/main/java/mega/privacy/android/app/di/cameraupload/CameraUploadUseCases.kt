package mega.privacy.android.app.di.cameraupload

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadFolderName
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.DefaultGetDefaultNodeHandle
import mega.privacy.android.app.domain.usecase.DefaultGetNodeFromCloud
import mega.privacy.android.app.domain.usecase.DefaultGetPendingUploadList
import mega.privacy.android.app.domain.usecase.DefaultGetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.DefaultGetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.DefaultGetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.DefaultIsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.DefaultProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.DefaultSaveSyncRecordsToDB
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.GetChildMegaNode
import mega.privacy.android.app.domain.usecase.GetDefaultNodeHandle
import mega.privacy.android.app.domain.usecase.GetFingerprint
import mega.privacy.android.app.domain.usecase.GetNodeByFingerprint
import mega.privacy.android.app.domain.usecase.GetNodeByFingerprintAndParentNode
import mega.privacy.android.app.domain.usecase.GetNodeFromCloud
import mega.privacy.android.app.domain.usecase.GetNodesByOriginalFingerprint
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.app.domain.usecase.GetPendingUploadList
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.ProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.SaveSyncRecordsToDB
import mega.privacy.android.app.domain.usecase.SetOriginalFingerprint
import mega.privacy.android.app.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileRepository
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.DefaultBackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.DefaultCheckCameraUpload
import mega.privacy.android.domain.usecase.DefaultCheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.DefaultClearSyncRecords
import mega.privacy.android.domain.usecase.DefaultCompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultDisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DefaultDisableMediaUploadSettings
import mega.privacy.android.domain.usecase.DefaultGetGPSCoordinates
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByPath
import mega.privacy.android.domain.usecase.DefaultGetUploadFolderHandle
import mega.privacy.android.domain.usecase.DefaultIsChargingRequired
import mega.privacy.android.domain.usecase.DefaultRenamePrimaryFolder
import mega.privacy.android.domain.usecase.DefaultRenameSecondaryFolder
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.DefaultResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetPrimaryTimeline
import mega.privacy.android.domain.usecase.DefaultResetSecondaryTimeline
import mega.privacy.android.domain.usecase.DefaultRestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.DefaultRestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.DefaultSetupPrimaryFolder
import mega.privacy.android.domain.usecase.DefaultSetupSecondaryFolder
import mega.privacy.android.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetCameraUploadFolderName
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetUploadFolderHandle
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.RenamePrimaryFolder
import mega.privacy.android.domain.usecase.RenameSecondaryFolder
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetPrimaryTimeline
import mega.privacy.android.domain.usecase.ResetSecondaryTimeline
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.StartCameraUpload
import mega.privacy.android.domain.usecase.StopCameraUpload
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.UpdateFolderDestinationBroadcast
import mega.privacy.android.domain.usecase.UpdateFolderIconBroadcast

/**
 * Provides the use case implementation for camera upload
 */
@Module(includes = [GetNodeModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
abstract class CameraUploadUseCases {

    companion object {
        /**
         * Provide the [HasCredentials] implementation
         */
        @Provides
        fun provideHasCredentials(cameraUploadRepository: CameraUploadRepository): HasCredentials =
            HasCredentials(cameraUploadRepository::doCredentialsExist)

        /**
         * Provide the [HasPreferences] implementation
         */
        @Provides
        fun provideHasPreferences(cameraUploadRepository: CameraUploadRepository): HasPreferences =
            HasPreferences(cameraUploadRepository::doPreferencesExist)

        /**
         * Provide the [IsCameraUploadSyncEnabled] implementation
         */
        @Provides
        fun provideIsCameraUploadSyncEnabled(cameraUploadRepository: CameraUploadRepository): IsCameraUploadSyncEnabled =
            IsCameraUploadSyncEnabled(cameraUploadRepository::isSyncEnabled)

        /**
         * Provide the [IsCameraUploadByWifi] implementation
         */
        @Provides
        fun provideIsCameraUploadByWifi(cameraUploadRepository: CameraUploadRepository): IsCameraUploadByWifi =
            IsCameraUploadByWifi(cameraUploadRepository::isSyncByWifi)

        /**
         * Provide the [GetChargingOnSizeString] implementation
         */
        @Provides
        fun provideGetChargingOnSizeString(cameraUploadRepository: CameraUploadRepository): GetChargingOnSizeString =
            GetChargingOnSizeString(cameraUploadRepository::getChargingOnSizeString)

        /**
         * Provide the [GetPendingSyncRecords] implementation
         */
        @Provides
        fun provideGetPendingSyncRecords(cameraUploadRepository: CameraUploadRepository): GetPendingSyncRecords =
            GetPendingSyncRecords(cameraUploadRepository::getPendingSyncRecords)

        /**
         * Provide the [UpdateFolderIconBroadcast]
         */
        @Provides
        fun provideUpdateFolderIconBroadcast(cameraUploadRepository: CameraUploadRepository): UpdateFolderIconBroadcast =
            UpdateFolderIconBroadcast(cameraUploadRepository::sendUpdateFolderIconBroadcast)

        /**
         * Provide the [UpdateFolderDestinationBroadcast]
         */
        @Provides
        fun provideUpdateFolderDestinationBroadcast(cameraUploadRepository: CameraUploadRepository): UpdateFolderDestinationBroadcast =
            UpdateFolderDestinationBroadcast(cameraUploadRepository::sendUpdateFolderDestinationBroadcast)

        // TODO CU-180 use refactored JobUtil gateway in camera upload repository and remove JobUtilWrapper/Context
        /**
         * Provide the [StopCameraUpload] implementation
         */
        @Provides
        fun provideStopCameraUpload(
            jobUtilWrapper: JobUtilWrapper,
            @ApplicationContext context: Context,
        ): StopCameraUpload = StopCameraUpload {
            jobUtilWrapper.fireStopCameraUploadJob(context)
        }

        // TODO CU-180 use refactored JobUtil gateway in camera upload repository and remove JobUtilWrapper/Context
        /**
         * Provide the [StartCameraUpload] implementation
         */
        @Provides
        fun provideStartCameraUpload(
            jobUtilWrapper: JobUtilWrapper,
            @ApplicationContext context: Context,
        ): StartCameraUpload = StartCameraUpload { shouldIgnoreAttributes ->
            jobUtilWrapper.fireCameraUploadJob(
                context,
                shouldIgnoreAttributes
            )
        }

        /**
         * Provide the [MediaLocalPathExists] implementation
         */
        @Provides
        fun provideMediaLocalPathExists(cameraUploadRepository: CameraUploadRepository): MediaLocalPathExists =
            MediaLocalPathExists { filePath, isSecondary ->
                cameraUploadRepository.doesLocalPathExist(
                    filePath,
                    isSecondary,
                    SyncRecordType.TYPE_ANY
                )
            }

        /**
         * Provide the [IsSecondaryFolderEnabled] implementation
         */
        @Provides
        fun provideIsSecondaryFolderEnabled(cameraUploadRepository: CameraUploadRepository): IsSecondaryFolderEnabled =
            IsSecondaryFolderEnabled(cameraUploadRepository::isSecondaryMediaFolderEnabled)

        /**
         * Provide the [DeleteSyncRecord] implementation
         */
        @Provides
        fun provideDeleteSyncRecord(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecord =
            DeleteSyncRecord(cameraUploadRepository::deleteSyncRecord)

        /**
         * Provide the [DeleteSyncRecordByLocalPath] implementation
         */
        @Provides
        fun provideDeleteSyncRecordByLocalPath(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecordByLocalPath =
            DeleteSyncRecordByLocalPath(cameraUploadRepository::deleteSyncRecordByLocalPath)

        /**
         * Provide the [DeleteSyncRecordByFingerprint] implementation
         */
        @Provides
        fun provideDeleteSyncRecordByFingerprint(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecordByFingerprint =
            DeleteSyncRecordByFingerprint(cameraUploadRepository::deleteSyncRecordByFingerprint)

        /**
         * Provide the [SetSecondaryFolderPath] implementation
         */
        @Provides
        fun provideSetSecondaryFolderPath(cameraUploadRepository: CameraUploadRepository): SetSecondaryFolderPath =
            SetSecondaryFolderPath(cameraUploadRepository::setSecondaryFolderPath)

        /**
         * Provide the [SetSyncLocalPath] implementation
         */
        @Provides
        fun provideSetSyncLocalPath(cameraUploadRepository: CameraUploadRepository): SetSyncLocalPath =
            SetSyncLocalPath(cameraUploadRepository::setSyncLocalPath)

        /**
         * Provide the [GetRemoveGps] implementation
         */
        @Provides
        fun provideGetRemoveGps(cameraUploadRepository: CameraUploadRepository): GetRemoveGps =
            GetRemoveGps(cameraUploadRepository::getRemoveGpsDefault)

        /**
         * Provide the [FileNameExists] implementation
         */
        @Provides
        fun provideFileNameExists(cameraUploadRepository: CameraUploadRepository): FileNameExists =
            FileNameExists { fileName, isSecondary ->
                cameraUploadRepository.doesFileNameExist(
                    fileName,
                    isSecondary,
                    SyncRecordType.TYPE_ANY
                )
            }

        /**
         * Provide the [KeepFileNames] implementation
         */
        @Provides
        fun provideKeepFileNames(cameraUploadRepository: CameraUploadRepository): KeepFileNames =
            KeepFileNames(cameraUploadRepository::getKeepFileNames)

        /**
         * Provide the [GetSyncRecordByFingerprint] implementation
         */
        @Provides
        fun provideGetSyncRecordByFingerprint(cameraUploadRepository: CameraUploadRepository): GetSyncRecordByFingerprint =
            GetSyncRecordByFingerprint(cameraUploadRepository::getSyncRecordByFingerprint)

        /**
         * Provide the [SaveSyncRecord] implementation
         */
        @Provides
        fun provideSaveSyncRecord(cameraUploadRepository: CameraUploadRepository): SaveSyncRecord =
            SaveSyncRecord(cameraUploadRepository::saveSyncRecord)

        /**
         * Provide the [SetSyncRecordPendingByPath] implementation
         */
        @Provides
        fun provideSetSyncRecordPendingByPath(cameraUploadRepository: CameraUploadRepository): SetSyncRecordPendingByPath =
            SetSyncRecordPendingByPath { localPath, isSecondary ->
                cameraUploadRepository.updateSyncRecordStatusByLocalPath(
                    SyncStatus.STATUS_PENDING.value,
                    localPath,
                    isSecondary
                )
            }

        /**
         * Provide the [GetVideoSyncRecordsByStatus] implementation
         */
        @Provides
        fun provideGetVideoSyncRecordsByStatus(cameraUploadRepository: CameraUploadRepository): GetVideoSyncRecordsByStatus =
            GetVideoSyncRecordsByStatus { cameraUploadRepository.getVideoSyncRecordsByStatus(it) }

        /**
         * Provide the [GetVideoQuality] implementation
         */
        @Provides
        fun provideGetVideoQuality(cameraUploadRepository: CameraUploadRepository): GetVideoQuality =
            GetVideoQuality { cameraUploadRepository.getVideoQuality().toInt() }

        /**
         * Provide the [GetFingerprint] implementation
         */
        @Provides
        fun provideGetFingerPrint(filesRepository: FilesRepository): GetFingerprint =
            GetFingerprint(filesRepository::getFingerprint)

        /**
         * Provide the [GetNodesByOriginalFingerprint] implementation
         */
        @Provides
        fun provideGetNodesByOriginalFingerprint(filesRepository: FilesRepository): GetNodesByOriginalFingerprint =
            GetNodesByOriginalFingerprint(filesRepository::getNodesByOriginalFingerprint)

        /**
         * Provide the [GetNodeByFingerprintAndParentNode] implementation
         */
        @Provides
        fun provideGetNodeByFingerprintAndParentNode(filesRepository: FilesRepository): GetNodeByFingerprintAndParentNode =
            GetNodeByFingerprintAndParentNode(filesRepository::getNodeByFingerprintAndParentNode)

        /**
         * Provide the [GetNodeByFingerprint] implementation
         */
        @Provides
        fun provideGetNodeByFingerprint(filesRepository: FilesRepository): GetNodeByFingerprint =
            GetNodeByFingerprint(filesRepository::getNodeByFingerprint)

        /**
         * Provide the [SetOriginalFingerprint] implementation
         *
         * @param filesRepository [FilesRepository]
         * @return [SetOriginalFingerprint]
         */
        @Provides
        fun provideSetOriginalFingerprint(filesRepository: FilesRepository): SetOriginalFingerprint =
            SetOriginalFingerprint(filesRepository::setOriginalFingerprint)

        /**
         * Provide the [GetParentMegaNode] implementation
         */
        @Provides
        fun provideGetParentMegaNode(filesRepository: FilesRepository): GetParentMegaNode =
            GetParentMegaNode(filesRepository::getParentNode)

        /**
         * Provide the [GetChildMegaNode] implementation
         */
        @Provides
        fun provideGetChildMegaNode(filesRepository: FilesRepository): GetChildMegaNode =
            GetChildMegaNode(filesRepository::getChildNode)

        /**
         * Provide the [SetPrimarySyncHandle] implementation
         */
        @Provides
        fun provideSetPrimarySyncHandle(cameraUploadRepository: CameraUploadRepository): SetPrimarySyncHandle =
            SetPrimarySyncHandle(cameraUploadRepository::setPrimarySyncHandle)

        /**
         * Provide the [SetSecondarySyncHandle] implementation
         */
        @Provides
        fun provideSetSecondarySyncHandle(cameraUploadRepository: CameraUploadRepository): SetSecondarySyncHandle =
            SetSecondarySyncHandle(cameraUploadRepository::setSecondarySyncHandle)

        /**
         * Provide the [IsNodeInRubbish] implementation
         */
        @Provides
        fun provideIsNodeInRubbish(fileRepository: FileRepository): IsNodeInRubbish =
            IsNodeInRubbish(fileRepository::isNodeInRubbish)

        /**
         * Provide the [ClearCacheDirectory] implementation
         */
        @Provides
        fun provideClearCacheDirectory(cameraUploadRepository: CameraUploadRepository): ClearCacheDirectory =
            ClearCacheDirectory(cameraUploadRepository::clearCacheDirectory)

        /**
         * Provide the [MonitorCameraUploadPauseState] implementation
         */
        @Provides
        fun provideMonitorCameraUploadPauseState(cameraUploadRepository: CameraUploadRepository): MonitorCameraUploadPauseState =
            MonitorCameraUploadPauseState(cameraUploadRepository::monitorCameraUploadPauseState)

        /**
         * Provide the [BroadcastUploadPauseState] implementation
         */
        @Provides
        fun provideBroadcastUploadPauseState(cameraUploadRepository: CameraUploadRepository): BroadcastUploadPauseState =
            BroadcastUploadPauseState(cameraUploadRepository::broadcastUploadPauseState)

        /**
         * Provide the [IsNodeInRubbishOrDeleted] implementation
         */
        @Provides
        fun provideIsNodeInRubbishOrDeleted(fileRepository: FileRepository): IsNodeInRubbishOrDeleted =
            IsNodeInRubbishOrDeleted(fileRepository::isNodeInRubbishOrDeleted)

        /**
         * Provide the [MonitorBatteryInfo] implementation
         */
        @Provides
        fun provideMonitorBatteryInfo(cameraUploadRepository: CameraUploadRepository): MonitorBatteryInfo =
            MonitorBatteryInfo(cameraUploadRepository::monitorBatteryInfo)

        /**
         * Provide the [MonitorChargingStoppedState] implementation
         */
        @Provides
        fun provideMonitorChargingStoppedState(cameraUploadRepository: CameraUploadRepository): MonitorChargingStoppedState =
            MonitorChargingStoppedState(cameraUploadRepository::monitorChargingStoppedInfo)

        /**
         * Provide the [CreateCameraUploadFolder] implementation
         */
        @Provides
        fun provideCreateCameraUploadFolder(fileRepository: FileRepository): CreateCameraUploadFolder =
            CreateCameraUploadFolder(fileRepository::createFolder)


    }

    /**
     * Provides the [CheckEnableCameraUploadsStatus] implementation
     */
    @Binds
    abstract fun bindCheckEnableCameraUploadsStatus(useCase: DefaultCheckEnableCameraUploadsStatus): CheckEnableCameraUploadsStatus

    /**
     * Provide the [GetNodeFromCloud] implementation
     */
    @Binds
    abstract fun bindGetNodeFromCloud(getNodeFromCloud: DefaultGetNodeFromCloud): GetNodeFromCloud

    /**
     * Provide the [GetDefaultNodeHandle] implementation
     */
    @Binds
    abstract fun bindGetDefaultNodeHandle(getDefaultNodeHandle: DefaultGetDefaultNodeHandle): GetDefaultNodeHandle

    /**
     * Provide the [ResetPrimaryTimeline] implementation
     */
    @Binds
    abstract fun bindResetPrimaryTimeline(resetPrimaryTimeline: DefaultResetPrimaryTimeline): ResetPrimaryTimeline

    /**
     * Provide the [ResetSecondaryTimeline] implementation
     */
    @Binds
    abstract fun bindResetSecondaryTimeline(resetSecondaryTimeline: DefaultResetSecondaryTimeline): ResetSecondaryTimeline

    /**
     * Provide the [ResetCameraUploadTimelines] implementation
     */
    @Binds
    abstract fun bindResetCameraUploadTimelines(resetCameraUploadTimelines: DefaultResetCameraUploadTimelines): ResetCameraUploadTimelines

    /**
     * Provide the [SetupPrimaryFolder] implementation
     */
    @Binds
    abstract fun bindSetupPrimaryFolder(setupPrimaryFolder: DefaultSetupPrimaryFolder): SetupPrimaryFolder

    /**
     * Provide the [SetupSecondaryFolder] implementation
     */
    @Binds
    abstract fun bindSetupSecondaryFolder(setupSecondaryFolder: DefaultSetupSecondaryFolder): SetupSecondaryFolder

    /**
     * Provide the [GetCameraUploadFolderName] implementation
     */
    @Binds
    abstract fun bindGetCameraUploadFolderName(getCameraUploadFolderName: DefaultGetCameraUploadFolderName): GetCameraUploadFolderName

    /**
     * Provide the [RenamePrimaryFolder] implementation
     */
    @Binds
    abstract fun bindRenamePrimaryFolder(renamePrimaryFolder: DefaultRenamePrimaryFolder): RenamePrimaryFolder

    /**
     * Provide the [RenameSecondaryFolder] implementation
     */
    @Binds
    abstract fun bindRenameSecondaryFolder(renameSecondaryFolder: DefaultRenameSecondaryFolder): RenameSecondaryFolder

    /**
     * Provide the [GetPrimarySyncHandle] implementation
     */
    @Binds
    abstract fun bindGetPrimarySyncHandle(getPrimarySyncHandle: DefaultGetPrimarySyncHandle): GetPrimarySyncHandle

    /**
     * Provide the [GetSecondarySyncHandle] implementation
     */
    @Binds
    abstract fun bindGetSecondarySyncHandle(getSecondarySyncHandle: DefaultGetSecondarySyncHandle): GetSecondarySyncHandle

    /**
     * Provide the [UpdateCameraUploadTimeStamp] implementation
     */
    @Binds
    abstract fun bindUpdateTimeStamp(updateTimeStamp: DefaultUpdateCameraUploadTimeStamp): UpdateCameraUploadTimeStamp

    /**
     * Provide the [GetCameraUploadLocalPath] implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPath(getLocalPath: DefaultGetCameraUploadLocalPath): GetCameraUploadLocalPath

    /**
     * Provide the [GetCameraUploadLocalPathSecondary] implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPathSecondary(getLocalPathSecondary: DefaultGetCameraUploadLocalPathSecondary): GetCameraUploadLocalPathSecondary

    /**
     * Provide the [GetCameraUploadSelectionQuery] implementation
     */
    @Binds
    abstract fun bindGetCameraUploadSelectionQuery(getSelectionQuery: DefaultGetCameraUploadSelectionQuery): GetCameraUploadSelectionQuery

    /**
     * Provide the [IsLocalPrimaryFolderSet] implementation
     */
    @Binds
    abstract fun bindIsLocalPrimaryFolderSet(isLocalPrimaryFolderSet: DefaultIsLocalPrimaryFolderSet): IsLocalPrimaryFolderSet

    /**
     * Provide the [IsLocalSecondaryFolderSet] implementation
     */
    @Binds
    abstract fun bindIsLocalSecondaryFolderSet(isLocalSecondaryFolderSet: DefaultIsLocalSecondaryFolderSet): IsLocalSecondaryFolderSet

    /**
     * Provide the [IsWifiNotSatisfied] implementation
     */
    @Binds
    abstract fun bindIsWifiNotSatisfied(isWifiNotSatisfied: DefaultIsWifiNotSatisfied): IsWifiNotSatisfied

    /**
     * Provide the [GetSyncFileUploadUris] implementation
     */
    @Binds
    abstract fun bindGetSyncFileUploadUris(getSyncFileUploadUris: DefaultGetSyncFileUploadUris): GetSyncFileUploadUris

    /**
     * Provide the [ShouldCompressVideo] implementation
     */
    @Binds
    abstract fun bindShouldCompressVideo(shouldCompressVideo: DefaultShouldCompressVideo): ShouldCompressVideo

    /**
     * Provide the [GetSyncRecordByPath] implementation
     */
    @Binds
    abstract fun bindGetSyncRecordByPath(getSyncRecordByPath: DefaultGetSyncRecordByPath): GetSyncRecordByPath

    /**
     * Provide the [ClearSyncRecords] implementation
     */
    @Binds
    abstract fun bindClearSyncRecords(clearSyncRecords: DefaultClearSyncRecords): ClearSyncRecords

    /**
     * Provide the [CompressedVideoPending] implementation
     */
    @Binds
    abstract fun bindCompressedVideoPending(compressedVideoPending: DefaultCompressedVideoPending): CompressedVideoPending

    /**
     * Provide the [GetGPSCoordinates] implementation
     */
    @Binds
    abstract fun bindGetGPSCoordinates(getGPSCoordinates: DefaultGetGPSCoordinates): GetGPSCoordinates

    /**
     * Provide the [IsChargingRequired] implementation
     */
    @Binds
    abstract fun bindIsChargingRequired(isChargingRequired: DefaultIsChargingRequired): IsChargingRequired

    /**
     * Provide the [SaveSyncRecordsToDB] implementation
     */
    @Binds
    abstract fun bindSaveSyncRecordsToDB(saveSyncRecordsToDB: DefaultSaveSyncRecordsToDB): SaveSyncRecordsToDB

    /**
     * Provide the [GetPendingUploadList] implementation
     */
    @Binds
    abstract fun bindGetPendingUploadList(getPendingUploadList: DefaultGetPendingUploadList): GetPendingUploadList

    /**
     * Provide the [ProcessMediaForUpload] implementation
     */
    @Binds
    abstract fun bindProcessMediaForUpload(processMediaForUpload: DefaultProcessMediaForUpload): ProcessMediaForUpload

    /**
     * Provide the [GetUploadFolderHandle] implementation
     */
    @Binds
    abstract fun bindGetUploadFolderHandle(getUploadFolderHandle: DefaultGetUploadFolderHandle): GetUploadFolderHandle

    /**
     * Provide the [ResetCameraUploadTimeStamps] implementation
     */
    @Binds
    abstract fun bindResetCameraUploadTimeStamps(resetCameraUploadTimeStamps: DefaultResetCameraUploadTimeStamps): ResetCameraUploadTimeStamps

    /**
     * Provide the [ResetMediaUploadTimeStamps] implementation
     */
    @Binds
    abstract fun bindResetMediaUploadTimeStamps(resetMediaUploadTimeStamps: DefaultResetMediaUploadTimeStamps): ResetMediaUploadTimeStamps

    /**
     * Provide the [DisableCameraUploadSettings] implementation
     */
    @Binds
    abstract fun bindDisableCameraUploadSettings(disableCameraUploadSettings: DefaultDisableCameraUploadSettings): DisableCameraUploadSettings

    /**
     * Provide the [DisableMediaUploadSettings] implementation
     */
    @Binds
    abstract fun bindDisableMediaUploadSettings(disableMediaUploadSettings: DefaultDisableMediaUploadSettings): DisableMediaUploadSettings

    /**
     * Provide the [CheckCameraUpload] implementation
     */
    @Binds
    abstract fun bindCheckCameraUpload(defaultCheckCameraUpload: DefaultCheckCameraUpload): CheckCameraUpload

    /**
     * Provide the [BackupTimeStampsAndFolderHandle] implementation
     */
    @Binds
    abstract fun bindBackupTimeStampsAndFolderHandle(defaultBackupTimeStampsAndFolderHandle: DefaultBackupTimeStampsAndFolderHandle): BackupTimeStampsAndFolderHandle

    /**
     * Provide the [RestorePrimaryTimestamps] implementation
     */
    @Binds
    abstract fun bindRestorePrimaryTimestamps(defaultRestorePrimaryTimestamps: DefaultRestorePrimaryTimestamps): RestorePrimaryTimestamps

    /**
     * Provide the [RestoreSecondaryTimestamps] implementation
     */
    @Binds
    abstract fun bindRestoreSecondaryTimestamps(defaultRestoreSecondaryTimestamps: DefaultRestoreSecondaryTimestamps): RestoreSecondaryTimestamps
}
