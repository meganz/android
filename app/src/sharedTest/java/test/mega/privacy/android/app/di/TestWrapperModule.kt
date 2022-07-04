package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.UtilWrapperModule
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.IsOnlineWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilWrapperModule::class]
)
object TestWrapperModule {

    val isOnlineWrapper = mock<IsOnlineWrapper>()
    val cameraUploadSyncManagerWrapper = mock<CameraUploadSyncManagerWrapper>()
    val cameraUploadsServiceWrapper = mock<CameraUploadsServiceWrapper>()
    val jobUtilWrapper = mock<JobUtilWrapper>()
    val permissionUtilWrapper = mock<PermissionUtilWrapper>()
    val getOfflineThumbnailFileWrapper = mock<GetOfflineThumbnailFileWrapper>()
    val fetchNodeWrapper = mock<FetchNodeWrapper>()

    @Provides
    fun provideIsOnlineWrapper(): IsOnlineWrapper = isOnlineWrapper

    @Provides
    fun provideCameraUploadSyncManagerWrapper(): CameraUploadSyncManagerWrapper =
        cameraUploadSyncManagerWrapper

    @Provides
    fun provideCameraUploadsServiceWrapper(): CameraUploadsServiceWrapper =
        cameraUploadsServiceWrapper

    @Provides
    fun provideJobUtilWrapper(): JobUtilWrapper = jobUtilWrapper

    @Provides
    fun providePermissionUtilWrapper(): PermissionUtilWrapper = permissionUtilWrapper

    @Provides
    fun provideGetOfflineThumbnailFileWrapper(): GetOfflineThumbnailFileWrapper =
        getOfflineThumbnailFileWrapper


    @Provides
    fun provideFetchNodeWrapper(): FetchNodeWrapper = fetchNodeWrapper
}