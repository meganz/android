package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.cameraupload.CameraUploadUseCases
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.app.domain.usecase.SetupDefaultSecondaryFolder
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetCameraUploadFolderName
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
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
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.UpdateFolderDestinationBroadcast
import mega.privacy.android.domain.usecase.UpdateFolderIconBroadcast
import mega.privacy.android.domain.usecase.camerauploads.GetNodeByFingerprintUseCase
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [CameraUploadUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestCameraUploadUseCases {

    @Provides
    fun provideHasCredentials() = mock<HasCredentials>()

    @Provides
    fun provideIsCameraUploadSyncEnabled() = mock<IsCameraUploadSyncEnabled>()

    @Provides
    fun provideGetPendingSyncRecords() = mock<GetPendingSyncRecords>()

    @Provides
    fun provideMediaLocalPathExists() = mock<MediaLocalPathExists>()

    @Provides
    fun provideIsSecondaryFolderEnabled() = mock<IsSecondaryFolderEnabled>()

    @Provides
    fun provideDeleteSyncRecord() = mock<DeleteSyncRecord>()

    @Provides
    fun provideDeleteSyncRecordByLocalPath() = mock<DeleteSyncRecordByLocalPath>()

    @Provides
    fun provideDeleteSyncRecordByFingerprint() = mock<DeleteSyncRecordByFingerprint>()

    @Provides
    fun provideFileNameExists() = mock<FileNameExists>()

    @Provides
    fun provideGetSyncRecordByFingerprint() = mock<GetSyncRecordByFingerprint>()

    @Provides
    fun provideSaveSyncRecord() = mock<SaveSyncRecord>()

    @Provides
    fun provideSetSyncRecordPendingByPath() = mock<SetSyncRecordPendingByPath>()

    @Provides
    fun provideGetVideoSyncRecordsByStatus() = mock<GetVideoSyncRecordsByStatus>()

    @Provides
    fun provideGetNodeByFingerprint() = mock<GetNodeByFingerprintUseCase>()

    @Provides
    fun provideGetParentMegaNode() = mock<GetParentMegaNode>()

    @Provides
    fun provideResetPrimaryTimeline() = mock<ResetPrimaryTimeline>()

    @Provides
    fun provideResetSecondaryTimeline() = mock<ResetSecondaryTimeline>()

    @Provides
    fun provideResetCameraUploadTimelines() = mock<ResetCameraUploadTimelines>()

    @Provides
    fun provideUpdateTimeStamp() = mock<UpdateCameraUploadTimeStamp>()

    @Provides
    fun provideUpdateFolderIconBroadcast() = mock<UpdateFolderIconBroadcast>()

    @Provides
    fun provideUpdateFolderDestinationBroadcast() = mock<UpdateFolderDestinationBroadcast>()

    @Provides
    fun provideShouldCompressVideo() = mock<ShouldCompressVideo>()

    @Provides
    fun provideGetSyncRecordByPath() = mock<GetSyncRecordByPath>()

    @Provides
    fun provideClearSyncRecords() = mock<ClearSyncRecords>()

    @Provides
    fun provideCompressedVideoPending() = mock<CompressedVideoPending>()

    @Provides
    fun provideGetGPSCoordinates() = mock<GetGPSCoordinates>()

    @Provides
    fun provideIsChargingRequired() = mock<IsChargingRequired>()

    @Provides
    fun provideSetPrimarySyncHandle() = mock<SetPrimarySyncHandle>()

    @Provides
    fun provideSetSecondarySyncHandle() = mock<SetSecondarySyncHandle>()

    @Provides
    fun provideIsNodeInRubbish() = mock<IsNodeInRubbish>()

    @Provides
    fun provideClearCacheDirectory() = mock<ClearCacheDirectory>()

    @Provides
    fun provideCheckEnableCameraUploadsStatus() = mock<CheckEnableCameraUploadsStatus>()

    @Provides
    fun provideResetCameraUploadTimeStamps() = mock<ResetCameraUploadTimeStamps>()

    @Provides
    fun provideResetMediaUploadTimeStamps() = mock<ResetMediaUploadTimeStamps>()

    @Provides
    fun provideSetupPrimaryFolder() = mock<SetupPrimaryFolder>()

    @Provides
    fun provideSetupSecondaryFolder() = mock<SetupSecondaryFolder>()

    @Provides
    fun provideSetupDefaultSecondaryFolder() = mock<SetupDefaultSecondaryFolder>()

    @Provides
    fun provideGetCameraUploadFolderName() = mock<GetCameraUploadFolderName>()

    @Provides
    fun provideRenamePrimaryFolder() = mock<RenamePrimaryFolder>()

    @Provides
    fun provideRenameSecondaryFolder() = mock<RenameSecondaryFolder>()

    @Provides
    fun provideDisableCameraUploadSettings() = mock<DisableCameraUploadSettings>()

    @Provides
    fun provideDisableMediaUploadSettings() = mock<DisableMediaUploadSettings>()

    @Provides
    fun provideDisableCameraUploadsInDatabase() = mock<DisableCameraUploadsInDatabase>()

    @Provides
    fun provideBackupTimeStampsAndFolderHandle() = mock<BackupTimeStampsAndFolderHandle>()

    @Provides
    fun provideRestorePrimaryTimestamps() = mock<RestorePrimaryTimestamps>()

    @Provides
    fun provideRestoreSecondaryTimestamps() = mock<RestoreSecondaryTimestamps>()

    @Provides
    fun provideMonitorCameraUploadPauseState() = mock<MonitorCameraUploadPauseState>()

    @Provides
    fun provideBroadcastUploadPauseState() = mock<BroadcastUploadPauseState>()

    @Provides
    fun provideIsNotEnoughQuota() = mock<IsNotEnoughQuota>()

    @Provides
    fun provideMonitorBatteryInfo() = mock<MonitorBatteryInfo>()

    @Provides
    fun provideMonitorChargingStoppedState() = mock<MonitorChargingStoppedState>()

    @Provides
    fun provideCreateCameraUploadFolder() = mock<CreateCameraUploadFolder>()

    @Provides
    fun provideCompressVideos() = mock<CompressVideos>()

    @Provides
    fun provideCreateCameraUploadTemporaryRootDirectory() =
        mock<CreateCameraUploadTemporaryRootDirectory>()

    @Provides
    fun provideBroadcastCameraUploadProgress() =
        mock<BroadcastCameraUploadProgress>()

    @Provides
    fun provideMonitorCameraUploadsProgress() =
        mock<MonitorCameraUploadProgress>()
}
