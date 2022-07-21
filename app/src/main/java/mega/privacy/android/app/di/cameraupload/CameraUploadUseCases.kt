package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultClearSyncRecords
import mega.privacy.android.domain.usecase.DefaultCompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultDeleteSyncRecord
import mega.privacy.android.domain.usecase.DefaultDeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DefaultDeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DefaultFileNameExists
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadSelectionQuery
import mega.privacy.android.domain.usecase.DefaultGetChargingOnSizeString
import mega.privacy.android.domain.usecase.DefaultGetPendingSyncRecords
import mega.privacy.android.domain.usecase.DefaultGetRemoveGps
import mega.privacy.android.app.domain.usecase.DefaultGetSyncFileUploadUris
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByPath
import mega.privacy.android.domain.usecase.DefaultGetVideoQuality
import mega.privacy.android.domain.usecase.DefaultGetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.DefaultIsChargingRequired
import mega.privacy.android.app.domain.usecase.DefaultIsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsLocalSecondaryFolderSet
import mega.privacy.android.domain.usecase.DefaultIsSecondaryFolderEnabled
import mega.privacy.android.app.domain.usecase.DefaultIsWifiNotSatisfied
import mega.privacy.android.domain.usecase.DefaultKeepFileNames
import mega.privacy.android.domain.usecase.DefaultMediaLocalPathExists
import mega.privacy.android.domain.usecase.DefaultSaveSyncRecord
import mega.privacy.android.domain.usecase.DefaultSetSecondaryFolderPath
import mega.privacy.android.domain.usecase.DefaultSetSyncLocalPath
import mega.privacy.android.domain.usecase.DefaultSetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp

/**
 * Provides the use case implementation for camera upload
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraUploadUseCases {

    companion object {

        /**
         * Provide the HasCredentials implementation
         */
        @Provides
        fun provideHasCredentials(cameraUploadRepository: CameraUploadRepository): HasCredentials =
            HasCredentials(cameraUploadRepository::doCredentialsExist)

        /**
         * Provide the HasPreferences implementation
         */
        @Provides
        fun provideHasPreferences(cameraUploadRepository: CameraUploadRepository): HasPreferences =
            HasPreferences(cameraUploadRepository::doPreferencesExist)

        /**
         * Provide the IsCameraUploadSyncEnabled implementation
         */
        @Provides
        fun provideIsCameraUploadSyncEnabled(cameraUploadRepository: CameraUploadRepository): IsCameraUploadSyncEnabled =
            IsCameraUploadSyncEnabled(cameraUploadRepository::isSyncEnabled)

        /**
         * Provide the IsCameraUploadByWifi implementation
         */
        @Provides
        fun provideIsCameraUploadByWifi(cameraUploadRepository: CameraUploadRepository): IsCameraUploadByWifi =
            IsCameraUploadByWifi(cameraUploadRepository::isSyncByWifi)
    }

    /**
     * Provide the UpdateTimeStamp implementation
     */
    @Binds
    abstract fun bindUpdateTimeStamp(updateTimeStamp: DefaultUpdateCameraUploadTimeStamp): UpdateCameraUploadTimeStamp

    /**
     * Provide the GetCameraUploadLocalPath implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPath(getLocalPath: DefaultGetCameraUploadLocalPath): GetCameraUploadLocalPath

    /**
     * Provide the GetCameraUploadLocalPathSecondary implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPathSecondary(getLocalPathSecondary: DefaultGetCameraUploadLocalPathSecondary): GetCameraUploadLocalPathSecondary

    /**
     * Provide the GetCameraUploadSelectionQuery implementation
     */
    @Binds
    abstract fun bindGetCameraUploadSelectionQuery(getSelectionQuery: DefaultGetCameraUploadSelectionQuery): GetCameraUploadSelectionQuery

    /**
     * Provide the IsLocalPrimaryFolderSet implementation
     */
    @Binds
    abstract fun bindIsLocalPrimaryFolderSet(isLocalPrimaryFolderSet: DefaultIsLocalPrimaryFolderSet): IsLocalPrimaryFolderSet

    /**
     * Provide the IsLocalSecondaryFolderSet implementation
     */
    @Binds
    abstract fun bindIsLocalSecondaryFolderSet(isLocalSecondaryFolderSet: DefaultIsLocalSecondaryFolderSet): IsLocalSecondaryFolderSet

    /**
     * Provide the IsSecondaryFolderEnabled implementation
     */
    @Binds
    abstract fun bindIsSecondaryFolderEnabled(isSecondaryFolderEnabled: DefaultIsSecondaryFolderEnabled): IsSecondaryFolderEnabled

    /**
     * Provide the IsWifiNotSatisfied implementation
     */
    @Binds
    abstract fun bindIsWifiNotSatisfied(isWifiNotSatisfied: DefaultIsWifiNotSatisfied): IsWifiNotSatisfied

    /**
     * Provide the DeleteSyncRecord implementation
     */
    @Binds
    abstract fun bindDeleteSyncRecord(deleteSyncRecord: DefaultDeleteSyncRecord): DeleteSyncRecord

    /**
     * Provide the DeleteSyncRecordByLocalPath implementation
     */
    @Binds
    abstract fun bindDeleteSyncRecordByLocalPath(deleteSyncRecordByLocalPath: DefaultDeleteSyncRecordByLocalPath): DeleteSyncRecordByLocalPath

    /**
     * Provide the DeleteSyncRecordByFingerprint implementation
     */
    @Binds
    abstract fun bindDeleteSyncRecordByFingerprint(deleteSyncRecordByFingerprint: DefaultDeleteSyncRecordByFingerprint): DeleteSyncRecordByFingerprint

    /**
     * Provide the SetSecondaryFolderPath implementation
     */
    @Binds
    abstract fun bindSetSecondaryFolderPath(setSecondaryFolderPath: DefaultSetSecondaryFolderPath): SetSecondaryFolderPath

    /**
     * Provide the SetSyncLocalPath implementation
     */
    @Binds
    abstract fun bindSetSyncLocalPath(setSyncLocalPath: DefaultSetSyncLocalPath): SetSyncLocalPath


    /**
     * Provide the GetSyncFileUploadUris implementation
     */
    @Binds
    abstract fun bindGetSyncFileUploadUris(getSyncFileUploadUris: DefaultGetSyncFileUploadUris): GetSyncFileUploadUris

    /**
     * Provide the ShouldCompressVideo implementation
     */
    @Binds
    abstract fun bindShouldCompressVideo(shouldCompressVideo: DefaultShouldCompressVideo): ShouldCompressVideo

    /**
     * Provide the GetSyncRecordByPath implementation
     */
    @Binds
    abstract fun bindGetSyncRecordByPath(getSyncRecordByPath: DefaultGetSyncRecordByPath): GetSyncRecordByPath

    /**
     * Provide the ClearSyncRecords implementation
     */
    @Binds
    abstract fun bindClearSyncRecords(clearSyncRecords: DefaultClearSyncRecords): ClearSyncRecords

    /**
     * Provide the GetRemoveGps implementation
     */
    @Binds
    abstract fun bindGetRemoveGps(getRemoveGps: DefaultGetRemoveGps): GetRemoveGps

    /**
     * Provide the FileNameExists implementation
     */
    @Binds
    abstract fun bindFileNameExists(fileNameExists: DefaultFileNameExists): FileNameExists

    /**
     * Provide the KeepFileNames implementation
     */
    @Binds
    abstract fun bindKeepFileNames(keepFileNames: DefaultKeepFileNames): KeepFileNames

    /**
     * Provide the GetPendingSyncRecords implementation
     */
    @Binds
    abstract fun bindGetPendingSyncRecords(getPendingSyncRecords: DefaultGetPendingSyncRecords): GetPendingSyncRecords

    /**
     * Provide the CompressedVideoPending implementation
     */
    @Binds
    abstract fun bindCompressedVideoPending(compressedVideoPending: DefaultCompressedVideoPending): CompressedVideoPending

    /**
     * Provide the GetSyncRecordByFingerprint implementation
     */
    @Binds
    abstract fun bindGetSyncRecordByFingerprint(getSyncRecordByFingerprint: DefaultGetSyncRecordByFingerprint): GetSyncRecordByFingerprint

    /**
     * Provide the SaveSyncRecord implementation
     */
    @Binds
    abstract fun bindSaveSyncRecord(saveSyncRecord: DefaultSaveSyncRecord): SaveSyncRecord

    /**
     * Provide the SetSyncRecordPendingByPath implementation
     */
    @Binds
    abstract fun bindSetSyncRecordPendingByPath(setSyncRecordPendingByPath: DefaultSetSyncRecordPendingByPath): SetSyncRecordPendingByPath

    /**
     * Provide the GetVideoSyncRecordsByStatus implementation
     */
    @Binds
    abstract fun bindGetVideoSyncRecordsByStatus(getVideoSyncRecordsByStatus: DefaultGetVideoSyncRecordsByStatus): GetVideoSyncRecordsByStatus

    /**
     * Provide the GetVideoQuality implementation
     */
    @Binds
    abstract fun bindGetVideoQuality(getVideoQuality: DefaultGetVideoQuality): GetVideoQuality

    /**
     * Provide the GetChargingOnSizeString implementation
     */
    @Binds
    abstract fun bindGetChargingOnSizeString(getChargingOnSize: DefaultGetChargingOnSizeString): GetChargingOnSizeString

    /**
     * Provide the IsChargingRequired implementation
     */
    @Binds
    abstract fun bindIsChargingRequired(isChargingRequired: DefaultIsChargingRequired): IsChargingRequired

    /**
     * Provide the MediaLocalPathExists implementation
     */
    @Binds
    abstract fun bindMediaLocalPathExists(mediaLocalPathExists: DefaultMediaLocalPathExists): MediaLocalPathExists
}
