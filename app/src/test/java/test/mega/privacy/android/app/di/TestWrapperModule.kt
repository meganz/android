package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.UtilWrapperModule
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.AvatarWrapper
import mega.privacy.android.app.utils.wrapper.BitmapFactoryWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.GetDocumentFileWrapper
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.IsOnWifiWrapper
import mega.privacy.android.app.utils.wrapper.IsOnlineWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.app.utils.wrapper.TimeWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilWrapperModule::class]
)
object TestWrapperModule {

    val isOnlineWrapper = mock<IsOnlineWrapper>()
    val isOnWifiWrapper = mock<IsOnWifiWrapper>()
    val getFullPathWrapper = mock<GetFullPathFileWrapper>()
    val getDocumentFileWrapper = mock<GetDocumentFileWrapper>()
    val cameraUploadSyncManagerWrapper = mock<CameraUploadSyncManagerWrapper>()
    val cameraUploadsServiceWrapper = mock<CameraUploadsServiceWrapper>()
    val jobUtilWrapper = mock<JobUtilWrapper>()
    val permissionUtilWrapper = mock<PermissionUtilWrapper>()
    val getOfflineThumbnailFileWrapper = mock<GetOfflineThumbnailFileWrapper>()
    val fetchNodeWrapper = mock<FetchNodeWrapper>()
    val timeWrapper = mock<TimeWrapper>()
    val avatarWrapper = mock<AvatarWrapper>()
    val bitmapFactoryWrapper = mock<BitmapFactoryWrapper>()

    @Provides
    fun provideIsOnlineWrapper(): IsOnlineWrapper = isOnlineWrapper

    @Provides
    fun provideIsOnWifiWrapper(): IsOnWifiWrapper = isOnWifiWrapper

    @Provides
    fun provideGetFullPathFileWrapper(): GetFullPathFileWrapper = getFullPathWrapper

    @Provides
    fun provideGetDocumentFileWrapper(): GetDocumentFileWrapper = getDocumentFileWrapper

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

    @Provides
    fun provideTimeWrapper(): TimeWrapper = timeWrapper

    @Provides
    fun provideAvatarWrapper(): AvatarWrapper = avatarWrapper

    @Provides
    fun provideBitmapFactoryWrapper(): BitmapFactoryWrapper = bitmapFactoryWrapper

    @Provides
    fun provideMegaNodeUtilWrapper(): MegaNodeUtilWrapper = mock()
}