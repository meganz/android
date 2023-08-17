package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadFolderName
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.DefaultBackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.DefaultCheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.DefaultClearSyncRecords
import mega.privacy.android.domain.usecase.DefaultCompressVideos
import mega.privacy.android.domain.usecase.DefaultCompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultDisableMediaUploadSettings
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByPath
import mega.privacy.android.domain.usecase.DefaultIsChargingRequired
import mega.privacy.android.domain.usecase.DefaultIsNotEnoughQuota
import mega.privacy.android.domain.usecase.DefaultRenamePrimaryFolder
import mega.privacy.android.domain.usecase.DefaultRenameSecondaryFolder
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.DefaultResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.DefaultResetPrimaryTimeline
import mega.privacy.android.domain.usecase.DefaultResetSecondaryTimeline
import mega.privacy.android.domain.usecase.DefaultRestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.DefaultRestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.DefaultSetPrimarySyncHandle
import mega.privacy.android.domain.usecase.DefaultSetSecondarySyncHandle
import mega.privacy.android.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetCameraUploadFolderName
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadProgress
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
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.UpdateFolderDestinationBroadcast
import mega.privacy.android.domain.usecase.UpdateFolderIconBroadcast

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
         * Provide the [MediaLocalPathExists] implementation
         */
        @Provides
        fun provideMediaLocalPathExists(cameraUploadRepository: CameraUploadRepository): MediaLocalPathExists =
            MediaLocalPathExists { filePath, isSecondary ->
                cameraUploadRepository.doesLocalPathExist(
                    filePath,
                    isSecondary
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
         * Provide the [FileNameExists] implementation
         */
        @Provides
        fun provideFileNameExists(cameraUploadRepository: CameraUploadRepository): FileNameExists =
            FileNameExists { fileName, isSecondary ->
                cameraUploadRepository.doesFileNameExist(
                    fileName,
                    isSecondary
                )
            }

        /**
         * Provide the [GetSyncRecordByFingerprint] implementation
         */
        @Provides
        fun provideGetSyncRecordByFingerprint(cameraUploadRepository: CameraUploadRepository): GetSyncRecordByFingerprint =
            GetSyncRecordByFingerprint(cameraUploadRepository::getSyncRecordByFingerprint)

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
         * Provide the [GetParentMegaNode] implementation
         */
        @Provides
        fun provideGetParentMegaNode(megaNodeRepository: MegaNodeRepository): GetParentMegaNode =
            GetParentMegaNode(megaNodeRepository::getParentNode)

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
         * Provide the [MonitorCameraUploadProgress] implementation
         */
        @Provides
        fun provideMonitorCameraUploadProgress(cameraUploadRepository: CameraUploadRepository): MonitorCameraUploadProgress =
            MonitorCameraUploadProgress(cameraUploadRepository::monitorCameraUploadProgress)

        /**
         * Provide the [BroadcastCameraUploadProgress] implementation
         */
        @Provides
        fun provideBroadcastCameraUploadProgress(cameraUploadRepository: CameraUploadRepository): BroadcastCameraUploadProgress =
            BroadcastCameraUploadProgress(cameraUploadRepository::broadcastCameraUploadProgress)

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
    }

    /**
     * Provides the [CheckEnableCameraUploadsStatus] implementation
     */
    @Binds
    abstract fun bindCheckEnableCameraUploadsStatus(useCase: DefaultCheckEnableCameraUploadsStatus): CheckEnableCameraUploadsStatus

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
     * Provide the [UpdateCameraUploadTimeStamp] implementation
     */
    @Binds
    abstract fun bindUpdateTimeStamp(updateTimeStamp: DefaultUpdateCameraUploadTimeStamp): UpdateCameraUploadTimeStamp

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
     * Provide the [IsChargingRequired] implementation
     */
    @Binds
    abstract fun bindIsChargingRequired(isChargingRequired: DefaultIsChargingRequired): IsChargingRequired

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
     * Provide the [DisableMediaUploadSettings] implementation
     */
    @Binds
    abstract fun bindDisableMediaUploadSettings(disableMediaUploadSettings: DefaultDisableMediaUploadSettings): DisableMediaUploadSettings

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
     * Provide the [CompressVideos] implementation
     */
    @Binds
    abstract fun bindCompressVideos(resetTotalUploads: DefaultCompressVideos): CompressVideos
}
