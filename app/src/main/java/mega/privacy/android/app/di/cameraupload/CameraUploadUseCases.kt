package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.app.domain.usecase.DefaultDeleteSyncRecord
import mega.privacy.android.app.domain.usecase.DefaultDeleteSyncRecordByFingerprint
import mega.privacy.android.app.domain.usecase.DefaultDeleteSyncRecordByLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.DefaultGetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.DefaultIsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsSecondaryFolderEnabled
import mega.privacy.android.app.domain.usecase.DefaultIsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.DefaultSetSecondaryFolderPath
import mega.privacy.android.app.domain.usecase.DefaultSetSyncLocalPath
import mega.privacy.android.app.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.app.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.app.domain.usecase.DeleteSyncRecord
import mega.privacy.android.app.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.app.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.HasCredentials
import mega.privacy.android.app.domain.usecase.HasPreferences
import mega.privacy.android.app.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.app.domain.usecase.SetSyncLocalPath
import mega.privacy.android.app.domain.usecase.ShouldCompressVideo
import mega.privacy.android.app.domain.usecase.UpdateCameraUploadTimeStamp

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
}
