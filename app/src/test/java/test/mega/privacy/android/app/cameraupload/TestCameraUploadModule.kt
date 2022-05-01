package test.mega.privacy.android.app.cameraupload

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.UtilWrapperModule
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilWrapperModule::class]
)
object TestCameraUploadModule {
    val permissionUtilWrapper = mock<PermissionUtilWrapper>()
    val jobUtilWrapper = mock<JobUtilWrapper>()
    val cameraUploadsServiceWrapper = mock<CameraUploadsServiceWrapper>()
    private val cameraUploadSyncManagerWrapper = mock<CameraUploadSyncManagerWrapper>()

    @Provides
    fun providePermissionUtilWrapper(): PermissionUtilWrapper = permissionUtilWrapper

    @Provides
    fun provideJobUtilWrapper(): JobUtilWrapper = jobUtilWrapper

    @Provides
    fun provideCameraUploadsServiceWrapper(): CameraUploadsServiceWrapper =
        cameraUploadsServiceWrapper

    @Provides
    fun provideCameraUploadSyncManagerWrapper(): CameraUploadSyncManagerWrapper =
        cameraUploadSyncManagerWrapper
}
