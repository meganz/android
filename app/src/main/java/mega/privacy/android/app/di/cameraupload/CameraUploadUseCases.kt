package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.app.jobservices.DefaultIsLocalPrimaryFolderSet
import mega.privacy.android.app.jobservices.IsLocalPrimaryFolderSet

/**
 * Provides the use case implementation for camera upload
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraUploadUseCases {

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
     * Provide the GetCameraUploadSelectionQuery implementation
     */
    @Binds
    abstract fun bindGetCameraUploadSelectionQuery(getSelectionQuery: DefaultGetCameraUploadSelectionQuery): GetCameraUploadSelectionQuery

    /**
     * Provide the IsLocalPrimaryFolderSet implementation
     */
    @Binds
    abstract fun bindIsLocalPrimaryFolderSet(isLocalPrimaryFolderSet: DefaultIsLocalPrimaryFolderSet): IsLocalPrimaryFolderSet

}
