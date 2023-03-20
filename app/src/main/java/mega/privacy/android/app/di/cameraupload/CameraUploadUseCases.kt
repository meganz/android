package mega.privacy.android.app.di.cameraupload

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
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
import mega.privacy.android.app.domain.usecase.DefaultSetupDefaultSecondaryFolder
import mega.privacy.android.app.domain.usecase.GetCameraUploadAttributes
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
import mega.privacy.android.app.domain.usecase.SetupDefaultSecondaryFolder
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.data.wrapper.JobUtilWrapper
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinates
import mega.privacy.android.domain.usecase.DefaultBackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.DefaultCheckCameraUpload
import mega.privacy.android.domain.usecase.DefaultCheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.DefaultClearSyncRecords
import mega.privacy.android.domain.usecase.DefaultCompressVideos
import mega.privacy.android.domain.usecase.DefaultCompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultCreateCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DefaultCreateTempFileAndRemoveCoordinates
import mega.privacy.android.domain.usecase.DefaultDeleteCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DefaultDisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DefaultDisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DefaultDisableMediaUploadSettings
import mega.privacy.android.domain.usecase.DefaultGetGPSCoordinates
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByPath
import mega.privacy.android.domain.usecase.DefaultGetUploadFolderHandle
import mega.privacy.android.domain.usecase.DefaultIsChargingRequired
import mega.privacy.android.domain.usecase.DefaultIsNotEnoughQuota
import mega.privacy.android.domain.usecase.DefaultRenamePrimaryFolder
import mega.privacy.android.domain.usecase.DefaultRenameSecondaryFolder
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.DefaultResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetPrimaryTimeline
import mega.privacy.android.domain.usecase.DefaultResetSecondaryTimeline
import mega.privacy.android.domain.usecase.DefaultResetTotalUploads
import mega.privacy.android.domain.usecase.DefaultRestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.DefaultRestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.DefaultSetPrimarySyncHandle
import mega.privacy.android.domain.usecase.DefaultSetSecondarySyncHandle
import mega.privacy.android.domain.usecase.DefaultSetupPrimaryFolder
import mega.privacy.android.domain.usecase.DefaultSetupSecondaryFolder
import mega.privacy.android.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.DeleteCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetCameraUploadFolderName
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import mega.privacy.android.domain.usecase.GetNumberOfPendingUploads
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetUploadFolderHandle
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCameraSyncEnabled
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorCameraUploadProgress
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.RenamePrimaryFolder
import mega.privacy.android.domain.usecase.RenameSecondaryFolder
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetPrimaryTimeline
import mega.privacy.android.domain.usecase.ResetSecondaryTimeline
import mega.privacy.android.domain.usecase.ResetTotalUploads
import mega.privacy.android.domain.usecase.RestartCameraUpload
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.StartCameraUpload
import mega.privacy.android.domain.usecase.StopCameraUpload
import mega.privacy.android.domain.usecase.StopCameraUploadSyncHeartbeat
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.UpdateFolderDestinationBroadcast
import mega.privacy.android.domain.usecase.UpdateFolderIconBroadcast
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOption
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMedia
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabled
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOption
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatus

/**
 * Provides the use case implementation for camera upload
 */
@Module(includes = [GetNodeModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class, ServiceComponent::class)
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
         * Provide the [HasCameraSyncEnabled] implementation
         */
        @Provides
        fun provideHasCameraSyncEnabled(cameraUploadRepository: CameraUploadRepository): HasCameraSyncEnabled =
            HasCameraSyncEnabled(cameraUploadRepository::doesSyncEnabledExist)

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

        /**
         * Provide the [StopCameraUploadSyncHeartbeat] implementation
         */
        @Provides
        fun provideStopCameraUploadSyncHeartbeat(
            jobUtilWrapper: JobUtilWrapper,
            @ApplicationContext context: Context,
        ): StopCameraUploadSyncHeartbeat = StopCameraUploadSyncHeartbeat {
            jobUtilWrapper.stopCameraUploadSyncHeartbeatWorkers(context)
        }


        /**
         * Provide the [StartCameraUpload] implementation
         */
        @Provides
        fun provideStartCameraUpload(
            jobUtilWrapper: JobUtilWrapper,
            @ApplicationContext context: Context,
        ): StartCameraUpload = StartCameraUpload {
            jobUtilWrapper.fireCameraUploadJob(
                context,
            )
        }

        /**
         * Provides the [RestartCameraUpload] implementation
         *
         * @param jobUtilWrapper [JobUtilWrapper]
         * @param context [Context]
         *
         * @return [RestartCameraUpload]
         */
        @Provides
        fun provideRestartCameraUpload(
            jobUtilWrapper: JobUtilWrapper,
            @ApplicationContext context: Context,
        ): RestartCameraUpload = RestartCameraUpload {
            jobUtilWrapper.fireRestartCameraUploadJob(
                context = context,
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
         * Provide the [AreLocationTagsEnabled] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [AreLocationTagsEnabled]
         */
        @Provides
        fun provideAreLocationTagsEnabled(cameraUploadRepository: CameraUploadRepository): AreLocationTagsEnabled =
            AreLocationTagsEnabled(cameraUploadRepository::areLocationTagsEnabled)

        /**
         * Provide the [SetLocationTagsEnabled] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [SetLocationTagsEnabled]
         */
        @Provides
        fun provideSetLocationTagsEnabled(cameraUploadRepository: CameraUploadRepository): SetLocationTagsEnabled =
            SetLocationTagsEnabled(cameraUploadRepository::setLocationTagsEnabled)

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
         * Provide the [GetFingerprint] implementation
         */
        @Provides
        fun provideGetFingerPrint(megaNodeRepository: MegaNodeRepository): GetFingerprint =
            GetFingerprint(megaNodeRepository::getFingerprint)

        /**
         * Provide the [GetNodesByOriginalFingerprint] implementation
         */
        @Provides
        fun provideGetNodesByOriginalFingerprint(megaNodeRepository: MegaNodeRepository): GetNodesByOriginalFingerprint =
            GetNodesByOriginalFingerprint(megaNodeRepository::getNodesByOriginalFingerprint)

        /**
         * Provide the [GetNodeByFingerprintAndParentNode] implementation
         */
        @Provides
        fun provideGetNodeByFingerprintAndParentNode(megaNodeRepository: MegaNodeRepository): GetNodeByFingerprintAndParentNode =
            GetNodeByFingerprintAndParentNode(megaNodeRepository::getNodeByFingerprintAndParentNode)

        /**
         * Provide the [GetNodeByFingerprint] implementation
         */
        @Provides
        fun provideGetNodeByFingerprint(megaNodeRepository: MegaNodeRepository): GetNodeByFingerprint =
            GetNodeByFingerprint(megaNodeRepository::getNodeByFingerprint)

        /**
         * Provide the [SetOriginalFingerprint] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [SetOriginalFingerprint]
         */
        @Provides
        fun provideSetOriginalFingerprint(megaNodeRepository: MegaNodeRepository): SetOriginalFingerprint =
            SetOriginalFingerprint(megaNodeRepository::setOriginalFingerprint)

        /**
         * Provide the [GetParentMegaNode] implementation
         */
        @Provides
        fun provideGetParentMegaNode(megaNodeRepository: MegaNodeRepository): GetParentMegaNode =
            GetParentMegaNode(megaNodeRepository::getParentNode)

        /**
         * Provide the [GetChildMegaNode] implementation
         */
        @Provides
        fun provideGetChildMegaNode(megaNodeRepository: MegaNodeRepository): GetChildMegaNode =
            GetChildMegaNode(megaNodeRepository::getChildNode)

        /**
         * Provide the [IsNodeInRubbish] implementation
         */
        @Provides
        fun provideIsNodeInRubbish(nodeRepository: NodeRepository): IsNodeInRubbish =
            IsNodeInRubbish(nodeRepository::isNodeInRubbish)

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
         * Provide the [MonitorCameraUploadProgress] implementation
         */
        @Provides
        fun provideMonitorCameraUploadProgress(cameraUploadRepository: CameraUploadRepository): MonitorCameraUploadProgress =
            MonitorCameraUploadProgress(cameraUploadRepository::monitorCameraUploadProgress)

        /**
         * Provide the [BroadcastUploadPauseState] implementation
         */
        @Provides
        fun provideBroadcastUploadPauseState(cameraUploadRepository: CameraUploadRepository): BroadcastUploadPauseState =
            BroadcastUploadPauseState(cameraUploadRepository::broadcastUploadPauseState)

        /**
         * Provide the [BroadcastCameraUploadProgress] implementation
         */
        @Provides
        fun provideBroadcastCameraUploadProgress(cameraUploadRepository: CameraUploadRepository): BroadcastCameraUploadProgress =
            BroadcastCameraUploadProgress(cameraUploadRepository::broadcastCameraUploadProgress)

        /**
         * Provide the [IsNodeInRubbishOrDeleted] implementation
         */
        @Provides
        fun provideIsNodeInRubbishOrDeleted(nodeRepository: NodeRepository): IsNodeInRubbishOrDeleted =
            IsNodeInRubbishOrDeleted(nodeRepository::isNodeInRubbishOrDeleted)

        /**
         * Provide the [GetCameraUploadAttributes] implementation
         */
        @Provides
        fun provideGetCameraUploadAttributes(cameraUploadRepository: CameraUploadRepository): GetCameraUploadAttributes =
            GetCameraUploadAttributes(cameraUploadRepository::getUserAttribute)

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
        fun provideCreateCameraUploadFolder(fileSystemRepository: FileSystemRepository): CreateCameraUploadFolder =
            CreateCameraUploadFolder(fileSystemRepository::createFolder)

        /**
         * Provide the [GetNumberOfPendingUploads] implementation
         */
        @Provides
        @Suppress("DEPRECATION")
        fun provideGetNumberOfPendingUploads(cameraUploadRepository: CameraUploadRepository): GetNumberOfPendingUploads =
            GetNumberOfPendingUploads(cameraUploadRepository::getNumberOfPendingUploads)

        /**
         * Provides the [GetUploadOption] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [GetUploadOption]
         */
        @Provides
        fun provideGetUploadOption(cameraUploadRepository: CameraUploadRepository): GetUploadOption =
            GetUploadOption(cameraUploadRepository::getUploadOption)

        /**
         * Provides the [SetUploadOption] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [SetUploadOption]
         */
        @Provides
        fun provideSetUploadOption(cameraUploadRepository: CameraUploadRepository): SetUploadOption =
            SetUploadOption(cameraUploadRepository::setUploadOption)

        /**
         * Provides the [GetUploadVideoQuality] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [GetUploadVideoQuality]
         */
        @Provides
        fun provideGetUploadVideoQuality(cameraUploadRepository: CameraUploadRepository): GetUploadVideoQuality =
            GetUploadVideoQuality(cameraUploadRepository::getUploadVideoQuality)

        /**
         * Provides the [SetUploadVideoQuality] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [SetUploadVideoQuality]
         */
        @Provides
        fun provideSetUploadVideoQuality(cameraUploadRepository: CameraUploadRepository): SetUploadVideoQuality =
            SetUploadVideoQuality(cameraUploadRepository::setUploadVideoQuality)

        /**
         * Provides the [SetUploadVideoSyncStatus] implementation
         *
         * @param cameraUploadRepository [CameraUploadRepository]
         *
         * @return [SetUploadVideoSyncStatus]
         */
        @Provides
        fun provideSetUploadVideoSyncStatus(cameraUploadRepository: CameraUploadRepository): SetUploadVideoSyncStatus =
            SetUploadVideoSyncStatus(cameraUploadRepository::setUploadVideoSyncStatus)

        /**
         * Provides the [ListenToNewMedia] implementation
         *
         * @param repository [CameraUploadRepository]
         *
         * @return [ListenToNewMedia]
         */
        @Provides
        fun provideListenToNewMedia(repository: CameraUploadRepository): ListenToNewMedia =
            ListenToNewMedia(repository::listenToNewMedia)

        /**
         * Provides the [IsChargingRequiredForVideoCompression] implementation
         *
         * @param repository [CameraUploadRepository]
         *
         * @return [IsChargingRequiredForVideoCompression]
         */
        @Provides
        fun provideIsChargingRequiredForVideoCompression(repository: CameraUploadRepository): IsChargingRequiredForVideoCompression =
            IsChargingRequiredForVideoCompression(repository::isChargingRequiredForVideoCompression)

        /**
         * Provides the [SetChargingRequiredForVideoCompression] implementation
         *
         * @param repository [CameraUploadRepository]
         *
         * @return [SetChargingRequiredForVideoCompression]
         */
        @Provides
        fun provideSetChargingRequiredForVideoCompression(repository: CameraUploadRepository): SetChargingRequiredForVideoCompression =
            SetChargingRequiredForVideoCompression(repository::setChargingRequiredForVideoCompression)

        /**
         * Provides the [GetVideoCompressionSizeLimit] implementation
         *
         * @param repository [CameraUploadRepository]
         *
         * @return [GetVideoCompressionSizeLimit]
         */
        @Provides
        fun provideGetVideoCompressionSizeLimit(repository: CameraUploadRepository): GetVideoCompressionSizeLimit =
            GetVideoCompressionSizeLimit(repository::getVideoCompressionSizeLimit)
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
     * Provide the [SetupDefaultSecondaryFolder] implementation
     */
    @Binds
    abstract fun bindSetupDefaultSecondaryFolder(defaultSetupDefaultSecondaryFolder: DefaultSetupDefaultSecondaryFolder): SetupDefaultSecondaryFolder

    /**
     * Provide the [SetPrimarySyncHandle] implementation
     */
    @Binds
    abstract fun bindSetPrimarySyncHandle(setPrimarySyncHandle: DefaultSetPrimarySyncHandle): SetPrimarySyncHandle

    /**
     * Provide the [SetSecondarySyncHandle] implementation
     */
    @Binds
    abstract fun bindSetSecondarySyncHandle(setSecondarySyncHandle: DefaultSetSecondarySyncHandle): SetSecondarySyncHandle

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
     * Provide the [IsNotEnoughQuota] implementation
     */
    @Binds
    abstract fun bindIsNotEnoughQuota(isNotEnoughQuota: DefaultIsNotEnoughQuota): IsNotEnoughQuota

    /**
     * Provide the [DisableCameraUploadSettings] implementation
     */
    @Binds
    abstract fun bindDisableCameraUploadSettings(disableCameraUploadSettings: DefaultDisableCameraUploadSettings): DisableCameraUploadSettings

    /**
     * Provide the [DisableCameraUploadsInDatabase] implementation
     *
     * @param disableCameraUploadsInDatabase [DefaultDisableCameraUploadsInDatabase]
     *
     * @return [DisableCameraUploadsInDatabase]
     */
    @Binds
    abstract fun bindDisableCameraUploadsInDatabase(disableCameraUploadsInDatabase: DefaultDisableCameraUploadsInDatabase): DisableCameraUploadsInDatabase

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

    /**
     * provide [CreateTempFileAndRemoveCoordinates]
     */
    @Binds
    abstract fun bindCreateTempFileAndRemoveCoordinates(createTempFileAndRemoveCoordinates: DefaultCreateTempFileAndRemoveCoordinates): CreateTempFileAndRemoveCoordinates

    /**
     * Provide the [ResetTotalUploads] implementation
     *
     * @param resetTotalUploads [DefaultResetTotalUploads]
     *
     * @return [ResetTotalUploads]
     */
    @Binds
    abstract fun bindResetTotalUploads(resetTotalUploads: DefaultResetTotalUploads): ResetTotalUploads

    /**
     * Provide the [CompressVideos] implementation
     */
    @Binds
    abstract fun bindCompressVideos(resetTotalUploads: DefaultCompressVideos): CompressVideos


    /**
     * Provide the [CreateCameraUploadTemporaryRootDirectory] implementation
     */
    @Binds
    abstract fun bindCreateCameraUploadTemporaryRootDirectory(implementation: DefaultCreateCameraUploadTemporaryRootDirectory): CreateCameraUploadTemporaryRootDirectory

    /**
     * Provide the [DeleteCameraUploadTemporaryRootDirectory] implementation
     */
    @Binds
    abstract fun bindDeleteCameraUploadTemporaryRootDirectory(implementation: DefaultDeleteCameraUploadTemporaryRootDirectory): DeleteCameraUploadTemporaryRootDirectory

}
