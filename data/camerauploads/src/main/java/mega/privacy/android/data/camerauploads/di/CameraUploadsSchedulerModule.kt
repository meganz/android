package mega.privacy.android.data.camerauploads.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.camerauploads.scheduler.CameraUploadsWorkSchedulerImpl
import mega.privacy.android.data.camerauploads.scheduler.CameraUploadsWorkerClassGateway
import mega.privacy.android.data.camerauploads.scheduler.CameraUploadsWorkerClassGatewayImpl
import mega.privacy.android.data.gateway.CameraUploadsWorkScheduler
import javax.inject.Singleton

/**
 * Binds the camera uploads scheduler that :data starts and observes the workers through.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CameraUploadsSchedulerModule {

    @Binds
    @Singleton
    abstract fun bindCameraUploadsWorkScheduler(
        implementation: CameraUploadsWorkSchedulerImpl,
    ): CameraUploadsWorkScheduler

    @Binds
    @Singleton
    abstract fun bindCameraUploadsWorkerClassGateway(
        implementation: CameraUploadsWorkerClassGatewayImpl,
    ): CameraUploadsWorkerClassGateway
}
