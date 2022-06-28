package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.DefaultIsCameraUploadRunning
import mega.privacy.android.app.domain.usecase.DefaultUpdateTimeStamp
import mega.privacy.android.app.domain.usecase.IsCameraUploadRunning
import mega.privacy.android.app.domain.usecase.UpdateTimeStamp

/**
 * Provides the use case implementation for camera upload
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraUploadUseCases {

    /**
     * Provide the IsCameraUploadRunning implementation
     */
    @Binds
    abstract fun bindIsCameraUploadRunning(isCameraUploadRunning: DefaultIsCameraUploadRunning): IsCameraUploadRunning

    /**
     * Provide the UpdateTimeStamp implementation
     */
    @Binds
    abstract fun bindUpdateTimeStamp(updateTimeStamp: DefaultUpdateTimeStamp): UpdateTimeStamp
}
