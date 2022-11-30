package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.cameraupload.CameraUploadUseCases
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
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadSettings
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.FileNameExists
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
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
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
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
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
    fun provideHasPreferences() = mock<HasPreferences>()

    @Provides
    fun provideIsCameraUploadSyncEnabled() = mock<IsCameraUploadSyncEnabled>()

    @Provides
    fun provideIsCameraUploadByWifi() = mock<IsCameraUploadByWifi>()

    @Provides
    fun provideGetChargingOnSizeString() = mock<GetChargingOnSizeString>()

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
    fun provideSetSecondaryFolderPath() = mock<SetSecondaryFolderPath>()

    @Provides
    fun provideSetSyncLocalPath() = mock<SetSyncLocalPath>()

    @Provides
    fun provideGetRemoveGps() = mock<GetRemoveGps>()

    @Provides
    fun provideFileNameExists() = mock<FileNameExists>()

    @Provides
    fun provideKeepFileNames() = mock<KeepFileNames>()

    @Provides
    fun provideGetSyncRecordByFingerprint() = mock<GetSyncRecordByFingerprint>()

    @Provides
    fun provideSaveSyncRecord() = mock<SaveSyncRecord>()

    @Provides
    fun provideSetSyncRecordPendingByPath() = mock<SetSyncRecordPendingByPath>()

    @Provides
    fun provideGetVideoSyncRecordsByStatus() = mock<GetVideoSyncRecordsByStatus>()

    @Provides
    fun provideGetVideoQuality() = mock<GetVideoQuality>()

    @Provides
    fun provideGetFingerPrint() = mock<GetFingerprint>()

    @Provides
    fun provideGetNodesByOriginalFingerprint() = mock<GetNodesByOriginalFingerprint>()

    @Provides
    fun provideGetNodeByFingerprintAndParentNode() = mock<GetNodeByFingerprintAndParentNode>()

    @Provides
    fun provideGetNodeByFingerprint() = mock<GetNodeByFingerprint>()

    @Provides
    fun provideSetOriginalFingerprint() = mock<SetOriginalFingerprint>()

    @Provides
    fun provideGetParentMegaNode() = mock<GetParentMegaNode>()

    @Provides
    fun provideGetChildMegaNode() = mock<GetChildMegaNode>()

    @Provides
    fun provideGetNodeFromCloud() = mock<GetNodeFromCloud>()

    @Provides
    fun provideResetPrimaryTimeline() = mock<ResetPrimaryTimeline>()

    @Provides
    fun provideResetSecondaryTimeline() = mock<ResetSecondaryTimeline>()

    @Provides
    fun provideResetCameraUploadTimelines() = mock<ResetCameraUploadTimelines>()

    @Provides
    fun provideUpdateTimeStamp() = mock<UpdateCameraUploadTimeStamp>()

    @Provides
    fun provideGetCameraUploadLocalPath() = mock<GetCameraUploadLocalPath>()

    @Provides
    fun provideGetCameraUploadLocalPathSecondary() = mock<GetCameraUploadLocalPathSecondary>()

    @Provides
    fun provideGetCameraUploadSelectionQuery() = mock<GetCameraUploadSelectionQuery>()

    @Provides
    fun provideIsLocalPrimaryFolderSet() = mock<IsLocalPrimaryFolderSet>()

    @Provides
    fun provideIsLocalSecondaryFolderSet() = mock<IsLocalSecondaryFolderSet>()

    @Provides
    fun provideIsWifiNotSatisfied() = mock<IsWifiNotSatisfied>()

    @Provides
    fun provideGetSyncFileUploadUris() = mock<GetSyncFileUploadUris>()

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
    fun provideSaveSyncRecordsToDB() = mock<SaveSyncRecordsToDB>()

    @Provides
    fun provideGetPendingUploadList() = mock<GetPendingUploadList>()

    @Provides
    fun provideProcessMediaForUpload() = mock<ProcessMediaForUpload>()

    @Provides
    fun provideGetDefaultNodeHandle() = mock<GetDefaultNodeHandle>()

    @Provides
    fun provideSetPrimarySyncHandle() = mock<SetPrimarySyncHandle>()

    @Provides
    fun provideSetSecondarySyncHandle() = mock<SetSecondarySyncHandle>()

    @Provides
    fun provideIsNodeInRubbish() = mock<IsNodeInRubbish>()

    @Provides
    fun provideClearCacheDirectory() = mock<ClearCacheDirectory>()

    @Provides
    fun provideGetPrimarySyncHandle() = mock<GetPrimarySyncHandle>()

    @Provides
    fun provideGetSecondarySyncHandle() = mock<GetSecondarySyncHandle>()

    @Provides
    fun provideCheckEnableCameraUploadsStatus() = mock<CheckEnableCameraUploadsStatus>()

    @Provides
    fun provideGetUploadFolderHandle() = mock<GetUploadFolderHandle>()

    @Provides
    fun provideResetCameraUploadTimeStamps() = mock<ResetCameraUploadTimeStamps>()

    @Provides
    fun provideResetMediaUploadTimeStamps() = mock<ResetMediaUploadTimeStamps>()

    @Provides
    fun provideDisableCameraUploadSettings() = mock<DisableCameraUploadSettings>()

    @Provides
    fun provideDisableMediaUploadSettings() = mock<DisableMediaUploadSettings>()

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
    fun provideMonitorBatteryInfo() = mock<MonitorBatteryInfo>()

    @Provides
    fun provideMonitorChargingStoppedState() = mock<MonitorChargingStoppedState>()
}
